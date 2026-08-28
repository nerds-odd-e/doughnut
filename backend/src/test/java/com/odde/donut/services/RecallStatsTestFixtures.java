package com.odde.donut.services;

import com.odde.donut.controllers.dto.RecallStatsDTO;
import com.odde.donut.entities.AnswerOutcome;
import com.odde.donut.entities.Grade;
import com.odde.donut.entities.QuestionType;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared builders/lookups for recall-stats aggregation tests so each test class stays under the
 * 250-line limit.
 */
final class RecallStatsTestFixtures {
  // day 0 = 1989-01-01; day 9 = 1989-01-10; day 10 = 1989-01-11 (UTC).

  /**
   * "Today" for the {@link RecallPaceAggregator}/{@link RecallMorningHalfIndex} baseline-window
   * family of tests (slices 21.1, 21.3, 21.4): day 100, with the pace/lapse baseline window being
   * the same trailing {@code [today - 63, today - 4]} window {@link RecallPaceAggregator} itself
   * uses.
   */
  static final int WARMED_UP_BASELINE_TODAY = 100;

  static final int WARMED_UP_BASELINE_WINDOW_START = WARMED_UP_BASELINE_TODAY - 63; // 37
  static final int WARMED_UP_BASELINE_WINDOW_END = WARMED_UP_BASELINE_TODAY - 4; // 96
  static final LocalDate WARMED_UP_BASELINE_TODAY_DATE =
      LocalDate.of(1989, 1, 1).plusDays(WARMED_UP_BASELINE_TODAY);
  static final int WARMED_UP_BASELINE_MS = 5000;

  private RecallStatsTestFixtures() {}

  /**
   * One trailing baseline day: two items, each with a pre-existing baseline from day 0, answered
   * again on {@code day} so that day contributes exactly two residuals (needed for the consistency
   * spread baseline's own {@code >= 2 residuals/day} qualification) with a non-degenerate pace and
   * lapse spread across days. Shared by {@code RecallMorningHalfIndexTest} and {@code
   * RecallSplitHalfReliabilityTest}.
   */
  static void addWarmedUpBaselineDay(List<RecallAnswerRow> rows, int day, int itemIdBase) {
    int itemA = itemIdBase;
    int itemB = itemIdBase + 1;
    rows.add(answered(utc(0, 6), WARMED_UP_BASELINE_MS, true, null, itemA));
    rows.add(answered(utc(0, 7), WARMED_UP_BASELINE_MS, true, null, itemB));
    boolean evenDay = (day - WARMED_UP_BASELINE_WINDOW_START) % 2 == 0;
    int onTaskMsA = evenDay ? 4500 : 6000; // gives every baseline day a non-zero pace spread
    int onTaskMsB = evenDay ? 15000 : WARMED_UP_BASELINE_MS; // itemB lapses on half the days
    rows.add(answered(utc(day, 10), onTaskMsA, true, null, itemA));
    rows.add(answered(utc(day, 11), onTaskMsB, true, null, itemB));
  }

  /**
   * Full warmed-up baseline history across the trailing window: two fresh items per day, so every
   * baseline day contributes exactly two residuals.
   */
  static List<RecallAnswerRow> warmedUpBaselines() {
    List<RecallAnswerRow> rows = new ArrayList<>();
    int itemId = 2000;
    for (int day = WARMED_UP_BASELINE_WINDOW_START; day <= WARMED_UP_BASELINE_WINDOW_END; day++) {
      addWarmedUpBaselineDay(rows, day, itemId);
      itemId += 2;
    }
    return rows;
  }

  static RecallAnswerRow answered(
      Timestamp answerAt, Integer thinkingTimeMs, boolean correct, Timestamp promptAt) {
    return answered(answerAt, thinkingTimeMs, correct, promptAt, null);
  }

  static RecallAnswerRow answered(
      Timestamp answerAt,
      Integer thinkingTimeMs,
      boolean correct,
      Timestamp promptAt,
      Integer memoryTrackerId) {
    return answered(answerAt, thinkingTimeMs, correct, promptAt, memoryTrackerId, null);
  }

  static RecallAnswerRow answered(
      Timestamp answerAt,
      Integer thinkingTimeMs,
      boolean correct,
      Timestamp promptAt,
      Integer memoryTrackerId,
      Double retrievability) {
    return answered(
        answerAt,
        thinkingTimeMs,
        correct,
        promptAt,
        memoryTrackerId,
        retrievability,
        QuestionType.MCQ);
  }

  static RecallAnswerRow answered(
      Timestamp answerAt,
      Integer thinkingTimeMs,
      boolean correct,
      Timestamp promptAt,
      Integer memoryTrackerId,
      Double retrievability,
      QuestionType questionType) {
    return new RecallAnswerRow(
        answerAt,
        null,
        Grade.fromCorrect(correct),
        thinkingTimeMs,
        promptAt != null ? promptAt : answerAt,
        memoryTrackerId,
        retrievability,
        questionType);
  }

  static RecallAnswerRow overlapAnswered(Timestamp answerAt) {
    return new RecallAnswerRow(
        answerAt, AnswerOutcome.OVERLAP, null, null, answerAt, null, null, QuestionType.MCQ);
  }

  static Timestamp utc(int day, int hour) {
    return Timestamp.from(
        ZonedDateTime.of(1989, 1, 1, hour, 0, 0, 0, ZoneId.of("UTC")).plusDays(day).toInstant());
  }

  static RecallStatsDTO aggregate(List<RecallAnswerRow> recent, Timestamp now) {
    return RecallStatsService.aggregateRows(recent, recent, ZoneId.of("UTC"), now);
  }

  static RecallStatsDTO aggregateZone(List<RecallAnswerRow> recent, ZoneId zoneId, Timestamp now) {
    return RecallStatsService.aggregateRows(recent, recent, zoneId, now);
  }

  static RecallStatsDTO.DayAvgResponseTime dayAvg(RecallStatsDTO dto, String date) {
    return dto.getTrend().stream().filter(t -> t.getDate().equals(date)).findFirst().orElseThrow();
  }

  static RecallStatsDTO.DayRetention dayRet(RecallStatsDTO dto, String date) {
    return dto.getRetentionTrend().stream()
        .filter(t -> t.getDate().equals(date))
        .findFirst()
        .orElseThrow();
  }

  static long calendarCount(RecallStatsDTO dto, String date) {
    return dto.getCalendar().stream()
        .filter(c -> c.getDate().equals(date))
        .mapToInt(RecallStatsDTO.DayCount::getCount)
        .sum();
  }
}
