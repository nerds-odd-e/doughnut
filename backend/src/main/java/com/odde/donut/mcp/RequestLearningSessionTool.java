package com.odde.donut.mcp;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.LearningSessionService;
import com.odde.donut.testability.TestabilitySettings;
import com.odde.donut.utils.TimezoneUtils;
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

@Component
public record RequestLearningSessionTool(
    LearningSessionService learningSessions,
    AuthorizationService authorization,
    TestabilitySettings testabilitySettings,
    JsonMapper objectMapper) {
  public static final String NAME = "request_learning_session";
  public static final String NO_SESSION_SIGNAL = "No Potential learning session is available.";

  public SyncToolSpecification specification() {
    return SyncToolSpecification.builder()
        .tool(
            McpSchema.Tool.builder()
                .name(NAME)
                .meta(McpToolAuthorization.readOnlyMetadata())
                .description(
                    """
                    When the learner asks to start commissioned learning, obtain the first available
                    Potential learning session in Donut's due order. Returns notebookId,
                    notebookName and requestMarkdown, containing the existing Learning Session
                    Request with its tutoring instructions, Session Items and Report format.
                    Tutor interactively from that Request. When finished or the learner asks to end,
                    use record_learning_session with Feedback for completed items only.
                    Do not automatically start another
                    session. If no session is available, tell the learner that there is none.
                    """)
                .annotations(McpServerConfiguration.READ_ONLY_TOOL_ANNOTATIONS)
                .inputSchema(
                    Map.of(
                        "type", "object",
                        "properties",
                            Map.of(
                                "timezone",
                                Map.of(
                                    "type",
                                    "string",
                                    "description",
                                    "The learner's IANA timezone, for example Asia/Tokyo.")),
                        "required", List.of("timezone")))
                .build())
        .callHandler(this::call)
        .build();
  }

  private McpSchema.CallToolResult call(
      McpTransportContext context, McpSchema.CallToolRequest request) {
    User user = (User) context.get(McpServerConfiguration.TRANSPORT_USER_KEY);
    if (request.arguments() == null
        || !(request.arguments().get("timezone") instanceof String timezone)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "timezone is required");
    }
    var zoneId = TimezoneUtils.parseTimezone(timezone);
    var now = testabilitySettings.getCurrentUTCTimestamp();
    var notebook = learningSessions.firstPotentialNotebook(user, now, zoneId);
    if (notebook.isEmpty()) {
      return McpSchema.CallToolResult.builder()
          .addContent(new McpSchema.TextContent(NO_SESSION_SIGNAL))
          .build();
    }
    try {
      authorization.assertAuthorization(user, notebook.get());
      var result =
          Map.of(
              "notebookId", notebook.get().getId(),
              "notebookName", notebook.get().getName(),
              "requestMarkdown",
                  learningSessions.request(user, notebook.get(), now, zoneId).getRequestMarkdown());
      return McpSchema.CallToolResult.builder()
          .addContent(new McpSchema.TextContent(objectMapper.writeValueAsString(result)))
          .structuredContent(result)
          .build();
    } catch (UnexpectedNoAccessRightException e) {
      throw McpToolArguments.rethrow(e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
