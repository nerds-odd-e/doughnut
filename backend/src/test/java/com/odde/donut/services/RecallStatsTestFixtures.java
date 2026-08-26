package com.odde.donut.services;

import com.odde.donut.controllers.dto.RecallStatsDTO;
import com.odde.donut.entities.AnswerOutcome;
import com.odde.donut.entities.Grade;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Shared builders/lookups for recall-stats aggregation tests so each test class stays under the
 * 250-line limit.
 */
final class RecallStatsTestFixtures {
  // day 0 = 1989-01-01; day 9 = 1989-01-10; day 10 = 1989-01-11 (UTC).

  private RecallStatsTestFixtures() {}

  static RecallAnswerRow answered(
      Timestamp answerAt, Integer thinkingTimeMs, boolean correct, Timestamp promptAt) {
    return new RecallAnswerRow(
        answerAt,
        null,
        Grade.fromCorrect(correct),
        thinkingTimeMs,
        promptAt != null ? promptAt : answerAt,
        null,
        null);
  }

  static RecallAnswerRow overlapAnswered(Timestamp answerAt) {
    return new RecallAnswerRow(answerAt, AnswerOutcome.OVERLAP, null, null, answerAt, null, null);
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
