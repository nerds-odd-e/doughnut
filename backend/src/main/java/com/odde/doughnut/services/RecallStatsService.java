package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.RecallStatsDTO;
import com.odde.doughnut.controllers.dto.RecallStatsDTO.AmPmResponseTime;
import com.odde.doughnut.controllers.dto.RecallStatsDTO.DayAvgResponseTime;
import com.odde.doughnut.controllers.dto.RecallStatsDTO.DayCount;
import com.odde.doughnut.controllers.dto.RecallStatsDTO.DayRetention;
import com.odde.doughnut.controllers.dto.RecallStatsDTO.HeadlineStats;
import com.odde.doughnut.controllers.dto.RecallStatsDTO.HourRetention;
import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.RecallPromptRepository;
import com.odde.doughnut.utils.TimestampOperations;
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

  @Autowired
  public RecallStatsService(RecallPromptRepository recallPromptRepository) {
    this.recallPromptRepository = recallPromptRepository;
  }

  public RecallStatsDTO compute(User user, ZoneId zoneId, Timestamp now) {
    List<RecallPrompt> recent =
        recallPromptRepository.findAnsweredRecallPromptsInTimeRange(
            user.getId(), minusDays(now, 365), now);
    List<RecallPrompt> allTime =
        recallPromptRepository.findAnsweredRecallPromptsInTimeRange(
            user.getId(), minusDays(now, 5 * 365), now);
    return aggregate(recent, allTime, zoneId, now);
  }

  static RecallStatsDTO aggregate(
      List<RecallPrompt> recent, List<RecallPrompt> allTime, ZoneId zoneId, Timestamp now) {
    LocalDate today = now.toInstant().atZone(zoneId).toLocalDate();

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

    for (RecallPrompt rp : recent) {
      Answer answer = rp.getAnswer();
      if (answer == null || answer.getCreatedAt() == null) {
        continue;
      }
      ZonedDateTime zdt = TimestampOperations.getZonedDateTime(answer.getCreatedAt(), zoneId);
      LocalDate localDate = zdt.toLocalDate();
      int wd = zdt.getDayOfWeek().getValue() - 1;
      int hour = zdt.getHour();

      boolean correct = Boolean.TRUE.equals(answer.getCorrect());
      weekdayHourCounts[wd][hour]++;
      hourAnswered[hour]++;
      if (correct) {
        weekdayHourCorrect[wd][hour]++;
        hourCorrect[hour]++;
        totalCorrect365++;
      }
      perDayRetention.computeIfAbsent(localDate, k -> new int[2])[0] += correct ? 1 : 0;
      perDayRetention.computeIfAbsent(localDate, k -> new int[2])[1] += 1;

      Optional<Long> rt = RecallStatsAggregator.responseTimeMs(rp);
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
    List<HourRetention> hourlyRetention =
        RecallStatsAggregator.buildHourlyRetention(hourCorrect, hourAnswered);

    int totalReviews365 = recent.size();
    Double retentionPct365 = RecallStatsAggregator.pct(totalCorrect365, totalReviews365);
    int reviewsToday = perDayRetention.getOrDefault(today, new int[] {0, 0})[1];

    HeadlineStats totals =
        RecallStatsAggregator.buildTotals(
            allTime,
            zoneId,
            today,
            totalReviews365,
            reviewsToday,
            retentionPct365,
            hourlyRetention);

    return new RecallStatsDTO(
        calendar,
        trend,
        retentionTrend,
        amPm,
        weekdayHourCounts,
        weekdayHourCorrect,
        hourlyRetention,
        totals);
  }

  private static Timestamp minusDays(Timestamp ts, int days) {
    return Timestamp.from(ts.toInstant().minus(java.time.Duration.ofDays(days)));
  }
}
