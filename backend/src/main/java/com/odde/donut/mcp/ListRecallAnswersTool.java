package com.odde.donut.mcp;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.odde.donut.entities.User;
import com.odde.donut.services.RecallAnswerCursor;
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
      - **cursor** (optional) — the **nextCursor** of the previous response, to continue its
        window; the window stays as the first call fixed it, so **days** is ignored

      What this returns (JSON):
      - **from**, **to** — the window as absolute instants; it ends at the first call
      - **nextCursor** — present only when more attempts remain; a default call can take several
        responses, so call again with it as **cursor** until a response has none to read the
        whole window, each attempt exactly once
      - **attempts** — each with **answeredAt** (instant), **correct** (as recorded),
        **outcome** when the spelling answer had a special outcome — **OVERLAP** (accepted as
        overlapping another note; counts as correct) or **ACCIDENTAL_MATCH** (incorrect, but
        named another note, whose recall schedule was adjusted as a confusion; listed once, under
        the note being recalled), **excludedAsAmbiguous** with the learner's **reason** when they
        excluded the attempt as ambiguous (**correct** still shows the recorded result, though
        recall scheduling and statistics leave the attempt out), **note** (current **title** and
        qualified **portablePath** of the note being recalled; when the attempt is recorded as
        incorrect, a spelling miss adds the note's full current **content** and a multiple-choice
        miss adds its bounded **graph**, shaped like get_note_graph's result with notes addressed
        by portablePath, within a fixed modest budget of about 1,000 tokens; correct attempts carry
        title and path only; when the learner can no longer read the note, **note** carries only
        **unavailable** with the reason — the note is in the trash, or the learner can no longer
        read its notebook, such as after unsubscribing — and the rest of the attempt stays), and
        for multiple choice **multipleChoice** with **stem**, all **choices**, the **selectedChoice**, the
        **expectedAnswer**, and the stored **validationRationale** when available; for spelling
        **spelling** with the learner's **typedAnswer** and the masked **prompt** derived from
        the note, or **promptUnavailable** explaining why the prompt as shown cannot be
        recovered (the note was edited after the answer, or the note is unavailable)

      Pass a note's portablePath unchanged to read_note, or to get_note_graph with a larger
      tokenLimit for more context.
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
                                "Positive number of days to look back. Defaults to 7."),
                            "cursor",
                            Map.of(
                                "type",
                                "string",
                                "description",
                                "nextCursor from the previous response, to continue its window."))))
                .build())
        .callHandler(this::call)
        .build();
  }

  private McpSchema.CallToolResult call(
      McpTransportContext context, McpSchema.CallToolRequest request) {
    Map<String, Object> arguments = request.arguments() == null ? Map.of() : request.arguments();
    User user = (User) context.get(McpServerConfiguration.TRANSPORT_USER_KEY);
    if (arguments.get("cursor") instanceof String cursor) {
      return continueFrom(user, cursor);
    }
    double days = ((Number) arguments.getOrDefault("days", DEFAULT_DAYS)).doubleValue();
    if (days <= 0) {
      return McpToolRefusal.because(
          "A positive number of days is required, such as 14. Omit days for the last 7 days.");
    }
    return McpJsonToolResult.of(
        objectMapper,
        recallAnswerHistory.recentAttempts(
            user,
            testabilitySettings.getCurrentUTCTimestamp().toInstant(),
            Duration.ofMillis(Math.round(days * Duration.ofDays(1).toMillis()))));
  }

  private McpSchema.CallToolResult continueFrom(User user, String text) {
    RecallAnswerCursor cursor;
    try {
      cursor = RecallAnswerCursor.decode(text);
    } catch (IllegalArgumentException e) {
      return McpToolRefusal.because(
          "The cursor cannot be read. Start again without a cursor, then pass each response's"
              + " nextCursor unchanged.");
    }
    return McpJsonToolResult.of(objectMapper, recallAnswerHistory.continueFrom(user, cursor));
  }
}
