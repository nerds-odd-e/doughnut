package com.odde.donut.mcp;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.odde.donut.entities.User;
import com.odde.donut.services.RecallAnswerHistoryService;
import com.odde.donut.testability.TestabilitySettings;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Component;

/** MCP {@code list_recall_answers}: the learner's recent recall attempts, newest first. */
@Component
public record ListRecallAnswersTool(
    RecallAnswerHistoryService recallAnswerHistory,
    TestabilitySettings testabilitySettings,
    JsonMapper objectMapper) {
  public static final String NAME = "list_recall_answers";
  private static final int DEFAULT_DAYS = 7;

  private static final String DESCRIPTION =
      """
      Lists the learner's own recall answers from the last **days** days (default 7), newest
      first, so you can investigate what they confuse and where notes or questions could improve.

      Arguments:
      - **days** (optional) — a positive number of days to look back; the window is that many
        24-hour periods ending at the call

      What this returns (JSON):
      - **from**, **to** — the window as absolute instants; it ends at the call
      - **attempts** — each with **answeredAt** (instant), **correct** (as recorded),
        **note** (current **title** and qualified **portablePath**), and for multiple choice
        **multipleChoice** with **stem**, all **choices**, the **selectedChoice**, the
        **expectedAnswer**, and the stored **validationRationale** when available

      Pass a note's portablePath unchanged to read_note or get_note_graph for more context.
      This changes nothing.\
      """;

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
                            "days",
                            Map.of(
                                "type",
                                "number",
                                "description",
                                "Positive number of days to look back. Defaults to 7."))))
                .build())
        .callHandler(this::call)
        .build();
  }

  private McpSchema.CallToolResult call(
      McpTransportContext context, McpSchema.CallToolRequest request) {
    double days =
        request.arguments() == null
            ? DEFAULT_DAYS
            : ((Number) request.arguments().getOrDefault("days", DEFAULT_DAYS)).doubleValue();
    if (days <= 0) {
      return McpToolRefusal.because(
          "A positive number of days is required, such as 14. Omit days for the last 7 days.");
    }
    User user = (User) context.get(McpServerConfiguration.TRANSPORT_USER_KEY);
    return McpJsonToolResult.of(
        objectMapper,
        recallAnswerHistory.recentAttempts(
            user,
            testabilitySettings.getCurrentUTCTimestamp().toInstant(),
            Duration.ofMillis(Math.round(days * Duration.ofDays(1).toMillis()))));
  }
}
