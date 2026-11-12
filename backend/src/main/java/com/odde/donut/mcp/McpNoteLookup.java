package com.odde.donut.mcp;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.odde.donut.algorithms.WikiLinkMarkdown;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.services.WikiLinkResolver;
import io.modelcontextprotocol.spec.McpSchema;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
class McpNoteLookup {
  private final WikiLinkResolver wikiLinkResolver;
  private final McpNoteCandidates mcpNoteCandidates;
  private final JsonMapper objectMapper;

  McpNoteLookup(
      WikiLinkResolver wikiLinkResolver,
      McpNoteCandidates mcpNoteCandidates,
      JsonMapper objectMapper) {
    this.wikiLinkResolver = wikiLinkResolver;
    this.mcpNoteCandidates = mcpNoteCandidates;
    this.objectMapper = objectMapper;
  }

  McpSchema.CallToolResult resolve(
      McpSchema.CallToolRequest request,
      User viewer,
      Function<Note, McpSchema.CallToolResult> onResolved) {
    Object argument = request.arguments() == null ? null : request.arguments().get("portablePath");
    if (!(argument instanceof String path) || path.isBlank()) {
      return unresolved(
          "portablePath is required. Supply a qualified destination such as Engineering:Practices/Code Review.");
    }
    Object scopeArgument = request.arguments().get("notebookName");
    String notebookName = scopeArgument instanceof String scope ? scope : null;
    if (!WikiLinkMarkdown.splitInner(path).portablePath().hasNotebookQualifier()
        && (notebookName == null || notebookName.isBlank())) {
      return unresolved(
          "Supply notebookName for an unqualified portablePath, or qualify it as Engineering:Practices/Code Review. MCP has no source note from which to infer notebook scope.");
    }
    return switch (wikiLinkResolver.classifyToken(path, notebookName, viewer)) {
      case WikiLinkResolver.CandidateCardinality.Resolved resolved ->
          onResolved.apply(resolved.destinationNote());
      case WikiLinkResolver.CandidateCardinality.Unresolved ignored ->
          unresolved(
              "No readable note matches portablePath. Check the qualified destination or use list_note_candidates.");
      case WikiLinkResolver.CandidateCardinality.Ambiguous ambiguous ->
          ambiguity(ambiguous.candidates(), viewer);
    };
  }

  private static McpSchema.CallToolResult unresolved(String message) {
    return McpSchema.CallToolResult.builder()
        .isError(true)
        .addContent(new McpSchema.TextContent(message))
        .build();
  }

  private McpSchema.CallToolResult ambiguity(List<Note> notes, User viewer) {
    String message =
        "Several readable notes match portablePath. Select a candidate's portablePath.";
    Map<String, Object> result =
        Map.of(
            "message",
            message,
            "candidates",
            notes.stream().map(note -> mcpNoteCandidates.describe(note, viewer)).toList());
    try {
      return McpSchema.CallToolResult.builder()
          .isError(true)
          .addContent(
              new McpSchema.TextContent(message + "\n" + objectMapper.writeValueAsString(result)))
          .build();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
