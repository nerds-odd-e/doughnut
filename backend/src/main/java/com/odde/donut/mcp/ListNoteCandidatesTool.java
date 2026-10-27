package com.odde.donut.mcp;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.odde.donut.controllers.dto.NoteSearchResult;
import com.odde.donut.controllers.dto.RelationshipLiteralSearchHit;
import com.odde.donut.controllers.dto.SearchTerm;
import com.odde.donut.entities.User;
import com.odde.donut.services.search.NoteSearchService;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * MCP {@code list_note_candidates}: delegates title/alias search to {@link NoteSearchService} and
 * returns the existing candidate wire shape in ranking order.
 */
@Component
public class ListNoteCandidatesTool {
  public static final String NAME = "list_note_candidates";
  public static final String EMPTY_SEARCH_SIGNAL = "No matching notes.";

  private static final String DESCRIPTION =
      """
      Lists every note whose title or alias matches the query text, as candidates to compare before choosing one.

      Search Behavior:
      - Matches the query text against note titles and aliases
      - Covers all user's notebooks and subscribed notebooks
      - Returns note candidates only, in the search's ranking order

      Response Format:
      - JSON array; each candidate has noteId, title, notebookName, folderName (omitted at notebook root), and distance
      - Candidates with equal titles are told apart by notebookName and folderName
      - If no note matches, returns "%s"
      - Use a candidate's noteId with 'read_note' to read it in full for citing, or with 'get_note_graph' for its context and connections

      For best results, search with a likely note title or a distinctive word from it."""
          .formatted(EMPTY_SEARCH_SIGNAL);

  private final NoteSearchService noteSearchService;
  private final JsonMapper objectMapper;

  public ListNoteCandidatesTool(NoteSearchService noteSearchService, JsonMapper objectMapper) {
    this.noteSearchService = noteSearchService;
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
                            "query",
                            Map.of(
                                "type",
                                "string",
                                "description",
                                "Search text matched against note titles and aliases, e.g. a note title or a keyword it contains.")),
                        "required",
                        List.of("query")))
                .build())
        .callHandler(this::call)
        .build();
  }

  private McpSchema.CallToolResult call(
      McpTransportContext transportContext, McpSchema.CallToolRequest request) {
    User user = (User) transportContext.get(McpServerConfiguration.TRANSPORT_USER_KEY);
    SearchTerm searchTerm = new SearchTerm();
    searchTerm.setSearchKey(queryArgument(request));
    searchTerm.setAllMyNotebooksAndSubscriptions(true);

    List<Map<String, Object>> candidates =
        noteSearchService.searchForNotes(user, searchTerm).stream()
            .filter(RelationshipLiteralSearchHit::isNote)
            .map(RelationshipLiteralSearchHit::getNoteSearchResult)
            .filter(Objects::nonNull)
            .map(ListNoteCandidatesTool::toCandidate)
            .toList();

    if (candidates.isEmpty()) {
      return textResult(EMPTY_SEARCH_SIGNAL);
    }
    try {
      return textResult(objectMapper.writeValueAsString(candidates));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static McpSchema.CallToolResult textResult(String text) {
    return McpSchema.CallToolResult.builder().addContent(new McpSchema.TextContent(text)).build();
  }

  private static String queryArgument(McpSchema.CallToolRequest request) {
    Map<String, Object> arguments = request.arguments();
    if (arguments == null) {
      return "";
    }
    Object query = arguments.get("query");
    return query instanceof String string ? string : "";
  }

  private static Map<String, Object> toCandidate(NoteSearchResult note) {
    Map<String, Object> candidate = new LinkedHashMap<>();
    candidate.put("noteId", note.getNoteTopology().getId());
    candidate.put("title", note.getNoteTopology().getTitle());
    candidate.put("notebookName", note.getNotebookName());
    if (note.getFolderName() != null) {
      candidate.put("folderName", note.getFolderName());
    }
    candidate.put("distance", note.getDistance());
    return candidate;
  }
}
