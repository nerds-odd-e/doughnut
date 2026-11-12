package com.odde.donut.mcp;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.NoteGraphService;
import com.odde.donut.services.focusContext.FocusContextResult;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * MCP {@code get_note_graph}: bounded focus context via {@link NoteGraphService} and {@link
 * com.odde.donut.services.focusContext.RetrievalConfig#forGraphApi}.
 */
@Component
public class GetNoteGraphTool {
  public static final String NAME = "get_note_graph";

  private static final String DESCRIPTION =
      """
      Retrieves structured **focus context** for one note: the focus note plus related notes discovered through wiki links, inbound references, and sampled folder peers.

      What this returns (JSON):
      - **focusNote** — The requested note (depth 0) with title, notebook, folder path, truncated **content** when needed, and lightweight lists: **outgoingLinks** and **inboundReferences** (wiki-style link strings), plus **sampleSiblings** (a small capped sample of peers in the same folder or notebook root).
      - **relatedNotes** — Notes reached within the post-focus share of the combined token budget and max traversal depth. Each entry includes **depth**, **retrievalPath** (how the note was reached from the focus), and truncated **content** when needed.

      Use cases:
      - Inspect how a note connects to the rest of the notebook before answering questions about it.
      - After search, pull bounded context around a candidate note without loading the whole notebook.

      Pass a qualified Portable path directly, for example Engineering:Practices/Code Review. A candidate's portablePath from 'list_note_candidates' can be used unchanged.
      For an unqualified title, alias, folder path or /Title, supply notebookName explicitly. A qualified path supplies its own notebook scope. Property selectors require the exact decoded key and return the containing note's graph.
      If several readable notes match, select one of the returned candidates and pass its portablePath unchanged.
      You MUST provide a valid token limit (combined budget for focus plus related note bodies). Example choices:
      1. Short focus note and a large context window → higher limit (e.g., 5000).
      2. Long focus note or a small context window → lower limit (e.g., 500); a large focus body consumes budget before related notes are included.

      Navigation pattern:
      1. Follow a known Portable path or use 'list_note_candidates' to list matching notes
      2. Pick a candidate's portablePath
      3. Use this tool to load focus context around that note
      4. Follow **outgoingLinks** / **relatedNotes** titles for deeper exploration if needed\
      """;

  private final NoteGraphService noteGraphService;
  private final JsonMapper objectMapper;
  private final McpNoteLookup mcpNoteLookup;

  public GetNoteGraphTool(
      NoteGraphService noteGraphService, JsonMapper objectMapper, McpNoteLookup mcpNoteLookup) {
    this.noteGraphService = noteGraphService;
    this.objectMapper = objectMapper;
    this.mcpNoteLookup = mcpNoteLookup;
  }

  public SyncToolSpecification specification() {
    return SyncToolSpecification.builder()
        .tool(
            McpSchema.Tool.builder()
                .name(NAME)
                .meta(McpToolAuthorization.readOnlyMetadata())
                .description(DESCRIPTION)
                .annotations(McpServerConfiguration.READ_ONLY_TOOL_ANNOTATIONS)
                .inputSchema(
                    Map.of(
                        "type",
                        "object",
                        "properties",
                        Map.of(
                            "portablePath",
                            Map.of(
                                "type",
                                "string",
                                "description",
                                "Portable path of the note to explore, e.g. Engineering:Practices/Code Review."),
                            "notebookName",
                            Map.of(
                                "type",
                                "string",
                                "description",
                                "Explicit notebook scope for an unqualified portablePath; a qualified path supplies its own scope."),
                            "tokenLimit",
                            Map.of(
                                "type",
                                "number",
                                "description",
                                "Approximate token budget for the focus note body plus all related note bodies combined. Pick a value that fits your remaining context window.")),
                        "required",
                        List.of("portablePath", "tokenLimit")))
                .build())
        .callHandler(this::call)
        .build();
  }

  private McpSchema.CallToolResult call(
      McpTransportContext transportContext, McpSchema.CallToolRequest request) {
    User user = (User) transportContext.get(McpServerConfiguration.TRANSPORT_USER_KEY);
    int tokenLimit = tokenLimitArgument(request);
    return mcpNoteLookup.resolve(
        request, user, destination -> graph(user, destination, tokenLimit));
  }

  private McpSchema.CallToolResult graph(User user, Note destination, int tokenLimit) {
    try {
      FocusContextResult graph = noteGraphService.getGraph(user, destination.getId(), tokenLimit);
      return McpSchema.CallToolResult.builder()
          .addContent(new McpSchema.TextContent(objectMapper.writeValueAsString(graph)))
          .build();
    } catch (UnexpectedNoAccessRightException e) {
      throw McpToolFailures.rethrow(e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static int tokenLimitArgument(McpSchema.CallToolRequest request) {
    Map<String, Object> arguments = request.arguments();
    if (arguments == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "tokenLimit must be provided as a number");
    }
    Object tokenLimit = arguments.get("tokenLimit");
    if (!(tokenLimit instanceof Number number)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "tokenLimit must be provided as a number");
    }
    int value = number.intValue();
    if (value == 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "tokenLimit must be a positive number");
    }
    if (value <= 5) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "tokenLimit too low to fetch any note");
    }
    return value;
  }
}
