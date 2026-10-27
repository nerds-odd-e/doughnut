package com.odde.donut.mcp;

import com.fasterxml.jackson.databind.json.JsonMapper;
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

      You MUST obtain a valid note ID first using 'list_note_candidates' unless the user explicitly provides a numeric note ID.
      You MUST provide a valid token limit (combined budget for focus plus related note bodies). Example choices:
      1. Short focus note and a large context window → higher limit (e.g., 5000).
      2. Long focus note or a small context window → lower limit (e.g., 500); a large focus body consumes budget before related notes are included.

      Navigation pattern:
      1. Use 'list_note_candidates' to list matching notes
      2. Pick a candidate and take its noteId
      3. Use this tool to load focus context around that note
      4. Follow **outgoingLinks** / **relatedNotes** titles for deeper exploration if needed""";

  private final NoteGraphService noteGraphService;
  private final JsonMapper objectMapper;

  public GetNoteGraphTool(NoteGraphService noteGraphService, JsonMapper objectMapper) {
    this.noteGraphService = noteGraphService;
    this.objectMapper = objectMapper;
  }

  public SyncToolSpecification specification() {
    return SyncToolSpecification.builder()
        .tool(
            McpSchema.Tool.builder()
                .name(NAME)
                .description(DESCRIPTION)
                .inputSchema(
                    Map.of(
                        "type",
                        "object",
                        "properties",
                        Map.of(
                            "noteId",
                            Map.of(
                                "type",
                                "number",
                                "description",
                                "Numeric ID of the note to explore. Obtain this from a 'list_note_candidates' candidate or user-provided ID."),
                            "tokenLimit",
                            Map.of(
                                "type",
                                "number",
                                "description",
                                "Approximate token budget for the focus note body plus all related note bodies combined. Pick a value that fits your remaining context window.")),
                        "required",
                        List.of("noteId", "tokenLimit")))
                .build())
        .callHandler(this::call)
        .build();
  }

  private McpSchema.CallToolResult call(
      McpTransportContext transportContext, McpSchema.CallToolRequest request) {
    User user = (User) transportContext.get(McpServerConfiguration.TRANSPORT_USER_KEY);
    try {
      FocusContextResult graph =
          noteGraphService.getGraph(
              user, McpToolArguments.noteId(request), tokenLimitArgument(request));
      return McpSchema.CallToolResult.builder()
          .addContent(new McpSchema.TextContent(objectMapper.writeValueAsString(graph)))
          .build();
    } catch (UnexpectedNoAccessRightException e) {
      throw McpToolArguments.rethrow(e);
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
