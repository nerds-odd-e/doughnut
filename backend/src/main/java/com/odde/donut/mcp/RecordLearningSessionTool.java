package com.odde.donut.mcp;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NotebookRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.oauth.AssistantAuthorizationConfiguration;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.LearningSessionService;
import com.odde.donut.testability.TestabilitySettings;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public record RecordLearningSessionTool(
    LearningSessionService learningSessions,
    NotebookRepository notebooks,
    AuthorizationService authorization,
    TestabilitySettings testabilitySettings,
    JsonMapper objectMapper) {
  public static final String NAME = "record_learning_session";

  public SyncToolSpecification specification() {
    return SyncToolSpecification.builder()
        .tool(
            McpSchema.Tool.builder()
                .name(NAME)
                .description(
                    """
                    Save a Learning Session Report for the notebook returned by request_learning_session.
                    When the session finishes or the learner asks to end early, submit the existing
                    Report markdown with Grade 1–4 and descriptive Feedback for completed Session Items
                    only. Leave unfinished items out; an empty report records nothing. The response
                    lists recordedItems and rejectedEntries so you can explain rejected titles or grades.
                    Do not automatically start another session. Hosted assistants require the
                    learning_session:record permission; notes:read alone cannot record reports.
                    """)
                .annotations(
                    McpSchema.ToolAnnotations.builder()
                        .readOnlyHint(false)
                        .destructiveHint(false)
                        .idempotentHint(false)
                        .openWorldHint(false)
                        .build())
                .inputSchema(
                    Map.of(
                        "type", "object",
                        "properties",
                            Map.of(
                                "notebookId",
                                    Map.of(
                                        "type",
                                        "integer",
                                        "description",
                                        "Notebook ID returned by request_learning_session."),
                                "reportMarkdown",
                                    Map.of(
                                        "type",
                                        "string",
                                        "description",
                                        "Existing Learning Session Report markdown for completed items only.")),
                        "required", List.of("notebookId", "reportMarkdown")))
                .build())
        .callHandler(this::call)
        .build();
  }

  private McpSchema.CallToolResult call(
      McpTransportContext context, McpSchema.CallToolRequest request) {
    Set<?> assistantScopes =
        (Set<?>) context.get(McpServerConfiguration.TRANSPORT_ASSISTANT_SCOPES_KEY);
    if (assistantScopes != null
        && !assistantScopes.contains(AssistantAuthorizationConfiguration.RECORD_SCOPE)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "learning_session:record permission is required");
    }
    if (request.arguments() == null
        || !(request.arguments().get("notebookId") instanceof Number notebookId)
        || !(request.arguments().get("reportMarkdown") instanceof String reportMarkdown)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "notebookId and reportMarkdown are required");
    }
    var notebook =
        notebooks
            .findById(notebookId.intValue())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notebook not found."));
    User user = (User) context.get(McpServerConfiguration.TRANSPORT_USER_KEY);
    try {
      authorization.assertAuthorization(user, notebook);
      var result =
          learningSessions.record(
              user, notebook, reportMarkdown, testabilitySettings.getCurrentUTCTimestamp());
      String serialized = objectMapper.writeValueAsString(result);
      return McpSchema.CallToolResult.builder()
          .addContent(new McpSchema.TextContent(serialized))
          .structuredContent(objectMapper.readValue(serialized, Object.class))
          .build();
    } catch (UnexpectedNoAccessRightException e) {
      throw McpToolArguments.rethrow(e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
