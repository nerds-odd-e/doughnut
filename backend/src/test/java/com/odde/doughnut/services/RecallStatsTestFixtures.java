package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.RecallStatsDTO;
import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.RecallPrompt;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Shared builders/lookups for {@link RecallStatsServiceTest} so the test class stays under the
 * 250-line limit.
 */
final class RecallStatsTestFixtures {
  // day 0 = 1989-01-01; day 9 = 1989-01-10; day 10 = 1989-01-11 (UTC).

  private RecallStatsTestFixtures() {}

  static RecallPrompt answered(
      Timestamp answerAt, Integer thinkingTimeMs, Boolean correct, Timestamp promptAt) {
    RecallPrompt rp = new RecallPrompt();
    rp.setCreatedAt(promptAt != null ? promptAt : answerAt);
    Answer answer = new Answer();
    answer.setCorrect(correct);
    answer.setThinkingTimeMs(thinkingTimeMs);
    answer.setCreatedAt(answerAt);
    rp.setAnswer(answer);
    return rp;
  }

  static Timestamp utc(int day, int hour) {
    return Timestamp.from(
        ZonedDateTime.of(1989, 1, 1, hour, 0, 0, 0, ZoneId.of("UTC")).plusDays(day).toInstant());
  }

  static RecallStatsDTO aggregate(List<RecallPrompt> recent, Timestamp now) {
    return RecallStatsService.aggregate(recent, recent, ZoneId.of("UTC"), now);
  }

  static RecallStatsDTO aggregateZone(List<RecallPrompt> recent, ZoneId zoneId, Timestamp now) {
    return RecallStatsService.aggregate(recent, recent, zoneId, now);
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
