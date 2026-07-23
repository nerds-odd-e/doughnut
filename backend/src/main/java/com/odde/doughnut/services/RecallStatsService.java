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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RecallStatsService {
  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final int MIN_SAMPLES = 3;
  private static final int BEST_WORST_MIN_ANSWERED = 5;
  private static final long THINKING_CAP_MS = 120_000L;
  private static final long DIFF_CAP_MS = 300_000L;
  private static final long MIN_VALID_MS = 1_000L;

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

      Optional<Long> rt = responseTimeMs(rp);
      if (rt.isPresent()) {
        perDayTimes.computeIfAbsent(localDate, k -> new ArrayList<>()).add(rt.get());
        amPmValues[amPmIndex(hour)].add(rt.get());
      }
    }

    List<DayCount> calendar = buildCalendar(today, perDayRetention);
    List<DayAvgResponseTime> trend = buildTrend(today, perDayTimes);
    List<DayRetention> retentionTrend = buildRetentionTrend(today, perDayRetention);
    AmPmResponseTime amPm = buildAmPm(amPmValues);
    List<HourRetention> hourlyRetention = buildHourlyRetention(hourCorrect, hourAnswered);

    int totalReviews365 = recent.size();
    Double retentionPct365 = pct(totalCorrect365, totalReviews365);
    int reviewsToday =
        (int)
            perDayRetention.entrySet().stream()
                .filter(e -> e.getKey().equals(today))
                .mapToInt(e -> e.getValue()[1])
                .sum();

    HeadlineStats totals =
        buildTotals(
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

  private static HeadlineStats buildTotals(
      List<RecallPrompt> allTime,
      ZoneId zoneId,
      LocalDate today,
      int totalReviews365,
      int reviewsToday,
      Double retentionPct365,
      List<HourRetention> hourlyRetention) {
    long totalTimeSpentMs = 0;
    TreeSet<LocalDate> dates = new TreeSet<>();
    for (RecallPrompt rp : allTime) {
      Answer answer = rp.getAnswer();
      if (answer == null || answer.getCreatedAt() == null) {
        continue;
      }
      Optional<Long> rt = responseTimeMs(rp);
      if (rt.isPresent()) {
        totalTimeSpentMs += rt.get();
      }
      dates.add(TimestampOperations.getZonedDateTime(answer.getCreatedAt(), zoneId).toLocalDate());
    }
    int currentStreak = currentStreak(dates, today);
    int longestStreak = longestStreak(dates);
    Integer bestHour = null;
    Double bestPct = null;
    Integer worstHour = null;
    Double worstPct = null;
    for (HourRetention hr : hourlyRetention) {
      if (hr.getAnsweredCount() == null || hr.getAnsweredCount() < BEST_WORST_MIN_ANSWERED) {
        continue;
      }
      Double pct = hr.getRetentionPct();
      if (pct == null) {
        continue;
      }
      if (bestPct == null || pct > bestPct) {
        bestPct = pct;
        bestHour = hr.getHour();
      }
      if (worstPct == null || pct < worstPct) {
        worstPct = pct;
        worstHour = hr.getHour();
      }
    }
    return new HeadlineStats(
        allTime.size(),
        totalReviews365,
        reviewsToday,
        retentionPct365,
        currentStreak,
        longestStreak,
        totalTimeSpentMs,
        bestHour,
        bestPct,
        worstHour,
        worstPct);
  }

  private static List<DayCount> buildCalendar(
      LocalDate today, Map<LocalDate, int[]> perDayRetention) {
    List<DayCount> calendar = new ArrayList<>();
    for (int i = 364; i >= 0; i--) {
      LocalDate date = today.minusDays(i);
      int count = 0;
      int[] ret = perDayRetention.get(date);
      if (ret != null) {
        count = ret[1];
      }
      calendar.add(new DayCount(date.format(ISO_DATE), count));
    }
    return calendar;
  }

  private static List<DayAvgResponseTime> buildTrend(
      LocalDate today, Map<LocalDate, List<Long>> perDayTimes) {
    List<DayAvgResponseTime> trend = new ArrayList<>();
    for (int i = 89; i >= 0; i--) {
      LocalDate date = today.minusDays(i);
      List<Long> values = perDayTimes.getOrDefault(date, List.of());
      Long avg = trimmedMean(values);
      trend.add(new DayAvgResponseTime(date.format(ISO_DATE), avg, values.size()));
    }
    return trend;
  }

  private static List<DayRetention> buildRetentionTrend(
      LocalDate today, Map<LocalDate, int[]> perDayRetention) {
    List<DayRetention> trend = new ArrayList<>();
    for (int i = 89; i >= 0; i--) {
      LocalDate date = today.minusDays(i);
      int[] ret = perDayRetention.getOrDefault(date, new int[] {0, 0});
      int correct = ret[0];
      int answered = ret[1];
      Double pct = answered >= MIN_SAMPLES ? pct(correct, answered) : null;
      trend.add(new DayRetention(date.format(ISO_DATE), pct, correct, answered, answered));
    }
    return trend;
  }

  private static AmPmResponseTime buildAmPm(List<Long>[] values) {
    return new AmPmResponseTime(
        trimmedMean(values[0]),
        values[0].size(),
        trimmedMean(values[1]),
        values[1].size(),
        trimmedMean(values[2]),
        values[2].size(),
        trimmedMean(values[3]),
        values[3].size());
  }

  private static List<HourRetention> buildHourlyRetention(int[] correct, int[] answered) {
    List<HourRetention> list = new ArrayList<>();
    for (int h = 0; h < 24; h++) {
      int c = correct[h];
      int a = answered[h];
      Double pct = a >= MIN_SAMPLES ? pct(c, a) : null;
      list.add(new HourRetention(h, pct, c, a));
    }
    return list;
  }

  private static int currentStreak(TreeSet<LocalDate> dates, LocalDate today) {
    int streak = 0;
    LocalDate d = today;
    while (dates.contains(d)) {
      streak++;
      d = d.minusDays(1);
    }
    return streak;
  }

  private static int longestStreak(TreeSet<LocalDate> dates) {
    int longest = 0;
    int run = 0;
    LocalDate prev = null;
    for (LocalDate cur : dates) {
      if (prev != null && cur.equals(prev.plusDays(1))) {
        run++;
      } else {
        run = 1;
      }
      longest = Math.max(longest, run);
      prev = cur;
    }
    return longest;
  }

  private static Optional<Long> responseTimeMs(RecallPrompt rp) {
    Answer answer = rp.getAnswer();
    if (answer == null) {
      return Optional.empty();
    }
    long value;
    if (answer.getThinkingTimeMs() != null) {
      value = answer.getThinkingTimeMs();
      if (value > THINKING_CAP_MS) {
        value = THINKING_CAP_MS;
      }
    } else {
      if (answer.getCreatedAt() == null || rp.getCreatedAt() == null) {
        return Optional.empty();
      }
      value = answer.getCreatedAt().getTime() - rp.getCreatedAt().getTime();
      if (value > DIFF_CAP_MS) {
        value = DIFF_CAP_MS;
      }
    }
    if (value < MIN_VALID_MS) {
      return Optional.empty();
    }
    return Optional.of(value);
  }

  private static Long trimmedMean(List<Long> values) {
    if (values.size() < MIN_SAMPLES) {
      return null;
    }
    List<Long> sorted = new ArrayList<>(values);
    Collections.sort(sorted);
    int n = sorted.size();
    int low = (int) Math.floor(n * 0.05);
    int high = (int) Math.ceil(n * 0.95);
    if (high <= low) {
      high = low + 1;
    }
    long sum = 0;
    int count = 0;
    for (int i = low; i < high && i < n; i++) {
      sum += sorted.get(i);
      count++;
    }
    return count == 0 ? null : Math.round((double) sum / count);
  }

  private static Double pct(int correct, int answered) {
    if (answered == 0) {
      return null;
    }
    return (correct * 100.0) / answered;
  }

  private static int amPmIndex(int hour) {
    if (hour >= 6 && hour < 12) {
      return 0; // morning
    }
    if (hour >= 12 && hour < 18) {
      return 1; // afternoon
    }
    if (hour >= 18) {
      return 2; // evening
    }
    return 3; // night [00,06)
  }

  private static Timestamp minusDays(Timestamp ts, int days) {
    return Timestamp.from(ts.toInstant().minus(java.time.Duration.ofDays(days)));
  }
}
