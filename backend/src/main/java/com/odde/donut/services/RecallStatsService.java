package com.odde.donut.services;

import com.odde.donut.controllers.dto.RecallStatsDTO;
import com.odde.donut.controllers.dto.RecallStatsDTO.AccuracyStats;
import com.odde.donut.controllers.dto.RecallStatsDTO.AmPmResponseTime;
import com.odde.donut.controllers.dto.RecallStatsDTO.DailyProbeDay;
import com.odde.donut.controllers.dto.RecallStatsDTO.DayAvgResponseTime;
import com.odde.donut.controllers.dto.RecallStatsDTO.DayCount;
import com.odde.donut.controllers.dto.RecallStatsDTO.DayRetention;
import com.odde.donut.controllers.dto.RecallStatsDTO.HeadlineStats;
import com.odde.donut.controllers.dto.RecallStatsDTO.PaceStats;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.DailyProbeRepository;
import com.odde.donut.entities.repositories.RecallPromptRepository;
import com.odde.donut.utils.TimestampOperations;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RecallStatsService {
  private final RecallPromptRepository recallPromptRepository;
  private final DailyProbeRepository dailyProbeRepository;

  @Autowired
  public RecallStatsService(
      RecallPromptRepository recallPromptRepository, DailyProbeRepository dailyProbeRepository) {
    this.recallPromptRepository = recallPromptRepository;
    this.dailyProbeRepository = dailyProbeRepository;
  }

  public RecallStatsDTO compute(User user, ZoneId zoneId, Timestamp now) {
    // One projection query over the all-time window (5y); derive the 1y "recent" set in Java.
    // The projection selects only the fields the aggregator needs, so Hibernate never hydrates
    // RecallPrompt entities or their eager associations — this is what avoids the production
    // N+1/timeout.
    Timestamp sinceYear = minusDays(now, 365);
    List<RecallAnswerRow> allTime = findAllTimeAnsweredRows(user, now);
    List<RecallAnswerRow> recent = new ArrayList<>();
    for (RecallAnswerRow r : allTime) {
      if (!r.answerCreatedAt().before(sinceYear)) {
        recent.add(r);
      }
    }
    List<DailyProbeDay> dailyProbe =
        user.getDailyProbeEnabled()
            ? DailyProbeDaySeries.from(dailyProbeRepository.findByUser(user), zoneId)
            : List.of();
    return aggregateRows(recent, allTime, zoneId, now, dailyProbe);
  }

  private List<RecallAnswerRow> findAllTimeAnsweredRows(User user, Timestamp now) {
    return recallPromptRepository.findAnsweredRecallAnswerRows(
        user.getId(), minusDays(now, 5 * 365), now);
  }

  static RecallStatsDTO aggregateRows(
      List<RecallAnswerRow> recent, List<RecallAnswerRow> allTime, ZoneId zoneId, Timestamp now) {
    return aggregateRows(recent, allTime, zoneId, now, List.of());
  }

  static RecallStatsDTO aggregateRows(
      List<RecallAnswerRow> recent,
      List<RecallAnswerRow> allTime,
      ZoneId zoneId,
      Timestamp now,
      List<DailyProbeDay> dailyProbe) {
    List<RecallAnswerRow> recentReviews = reviewsOnly(recent);
    List<RecallAnswerRow> allTimeReviews = reviewsOnly(allTime);
    LocalDate today = localToday(now, zoneId);

    Map<LocalDate, List<Long>> perDayTimes = new HashMap<>();
    Map<LocalDate, int[]> perDayRetention = new HashMap<>();
    int[][] weekdayHourCounts = new int[7][24];
    int[][] weekdayHourCorrect = new int[7][24];
    int[] hourCorrect = new int[24];
    int[] hourAnswered = new int[24];
    List<Long>[] amPmValues = new List[4];
    for (int i = 0; i < 4; i++) {
      amPmValues[i] = new ArrayList<>();
    }
    int totalCorrect365 = 0;
    int totalReviews365 = 0;
    List<RecallAnswerRow> todaysQualifyingRows = new ArrayList<>();

    RecallPaceAggregator.PaceResult paceResult =
        RecallPaceAggregator.compute(allTimeReviews, today, zoneId);

    for (RecallAnswerRow r : recentReviews) {
      if (r.answerCreatedAt() == null) {
        continue;
      }
      if (paceResult.implausiblyFastRows().contains(r)) {
        continue;
      }
      totalReviews365++;
      ZonedDateTime zdt = TimestampOperations.getZonedDateTime(r.answerCreatedAt(), zoneId);
      LocalDate localDate = zdt.toLocalDate();
      int wd = zdt.getDayOfWeek().getValue() - 1;
      int hour = zdt.getHour();

      if (localDate.equals(today)) {
        todaysQualifyingRows.add(r);
      }

      boolean correct = r.correct();
      weekdayHourCounts[wd][hour]++;
      hourAnswered[hour]++;
      if (correct) {
        weekdayHourCorrect[wd][hour]++;
        hourCorrect[hour]++;
        totalCorrect365++;
      }
      perDayRetention.computeIfAbsent(localDate, k -> new int[2])[0] += correct ? 1 : 0;
      perDayRetention.computeIfAbsent(localDate, k -> new int[2])[1] += 1;

      Optional<Long> rt = RecallStatsAggregator.responseTimeMs(r);
      if (rt.isPresent()) {
        perDayTimes.computeIfAbsent(localDate, k -> new ArrayList<>()).add(rt.get());
        amPmValues[RecallStatsAggregator.amPmIndex(hour)].add(rt.get());
      }
    }

    List<DayCount> calendar = RecallStatsAggregator.buildCalendar(today, perDayRetention);
    List<DayAvgResponseTime> trend = RecallStatsAggregator.buildTrend(today, perDayTimes);
    List<DayRetention> retentionTrend =
        RecallStatsAggregator.buildRetentionTrend(today, perDayRetention);
    AmPmResponseTime amPm = RecallStatsAggregator.buildAmPm(amPmValues);

    Double retentionPct365 = RecallStatsAggregator.pct(totalCorrect365, totalReviews365);
    int reviewsToday = perDayRetention.getOrDefault(today, new int[] {0, 0})[1];

    HeadlineStats totals =
        RecallStatsAggregator.buildTotals(
            allTimeReviews,
            zoneId,
            today,
            totalReviews365,
            reviewsToday,
            retentionPct365,
            hourCorrect,
            hourAnswered);
    PaceStats pace = paceResult.stats();
    List<RecallAnswerRow> allTimeQualifyingRows =
        allTimeReviews.stream().filter(r -> !paceResult.implausiblyFastRows().contains(r)).toList();
    AccuracyStats accuracy =
        RecallAccuracyAggregator.compute(
            todaysQualifyingRows, allTimeQualifyingRows, today, zoneId);

    return new RecallStatsDTO(
        calendar,
        trend,
        retentionTrend,
        amPm,
        weekdayHourCounts,
        weekdayHourCorrect,
        totals,
        pace,
        accuracy,
        dailyProbe);
  }

  private static List<RecallAnswerRow> reviewsOnly(List<RecallAnswerRow> rows) {
    return rows.stream().filter(RecallAnswerRow::countsAsReview).toList();
  }

  private static LocalDate localToday(Timestamp now, ZoneId zoneId) {
    return now.toInstant().atZone(zoneId).toLocalDate();
  }

  private static Timestamp minusDays(Timestamp ts, int days) {
    return Timestamp.from(ts.toInstant().minus(java.time.Duration.ofDays(days)));
  }
}
