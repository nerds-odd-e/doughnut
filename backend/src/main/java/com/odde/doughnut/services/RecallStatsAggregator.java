package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.RecallStatsDTO;
import com.odde.doughnut.controllers.dto.RecallStatsDTO.AmPmResponseTime;
import com.odde.doughnut.controllers.dto.RecallStatsDTO.DayAvgResponseTime;
import com.odde.doughnut.controllers.dto.RecallStatsDTO.DayCount;
import com.odde.doughnut.controllers.dto.RecallStatsDTO.DayRetention;
import com.odde.doughnut.controllers.dto.RecallStatsDTO.HeadlineStats;
import com.odde.doughnut.controllers.dto.RecallStatsDTO.HourRetention;
import com.odde.doughnut.utils.TimestampOperations;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Pure aggregation primitives that turn {@link RecallAnswerRow} projections into the stats
 * series/totals of {@link RecallStatsDTO}.
 */
final class RecallStatsAggregator {
  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final int MIN_SAMPLES = 3;
  private static final int BEST_WORST_MIN_ANSWERED = 5;
  private static final long THINKING_CAP_MS = 120_000L;
  private static final long DIFF_CAP_MS = 300_000L;
  private static final long MIN_VALID_MS = 1_000L;

  private RecallStatsAggregator() {}

  static HeadlineStats buildTotals(
      List<RecallAnswerRow> allTime,
      ZoneId zoneId,
      LocalDate today,
      int totalReviews365,
      int reviewsToday,
      Double retentionPct365,
      List<HourRetention> hourlyRetention) {
    long totalTimeSpentMs = 0;
    TreeSet<LocalDate> dates = new TreeSet<>();
    for (RecallAnswerRow r : allTime) {
      if (r.answerCreatedAt() == null) {
        continue;
      }
      Optional<Long> rt = responseTimeMs(r);
      if (rt.isPresent()) {
        totalTimeSpentMs += rt.get();
      }
      dates.add(TimestampOperations.getZonedDateTime(r.answerCreatedAt(), zoneId).toLocalDate());
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

  static List<DayCount> buildCalendar(LocalDate today, Map<LocalDate, int[]> perDayRetention) {
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

  static List<DayAvgResponseTime> buildTrend(
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

  static List<DayRetention> buildRetentionTrend(
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

  static AmPmResponseTime buildAmPm(List<Long>[] values) {
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

  static List<HourRetention> buildHourlyRetention(int[] correct, int[] answered) {
    List<HourRetention> list = new ArrayList<>();
    for (int h = 0; h < 24; h++) {
      int c = correct[h];
      int a = answered[h];
      Double pct = a >= MIN_SAMPLES ? pct(c, a) : null;
      list.add(new HourRetention(h, pct, c, a));
    }
    return list;
  }

  static int currentStreak(TreeSet<LocalDate> dates, LocalDate today) {
    int streak = 0;
    LocalDate d = today;
    while (dates.contains(d)) {
      streak++;
      d = d.minusDays(1);
    }
    return streak;
  }

  static int longestStreak(TreeSet<LocalDate> dates) {
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

  static Optional<Long> responseTimeMs(RecallAnswerRow r) {
    if (r.answerCreatedAt() == null) {
      return Optional.empty();
    }
    long value;
    if (r.thinkingTimeMs() != null) {
      value = r.thinkingTimeMs();
      if (value > THINKING_CAP_MS) {
        value = THINKING_CAP_MS;
      }
    } else {
      if (r.promptCreatedAt() == null) {
        return Optional.empty();
      }
      value = r.answerCreatedAt().getTime() - r.promptCreatedAt().getTime();
      if (value > DIFF_CAP_MS) {
        value = DIFF_CAP_MS;
      }
    }
    if (value < MIN_VALID_MS) {
      return Optional.empty();
    }
    return Optional.of(value);
  }

  static Long trimmedMean(List<Long> values) {
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

  static Double pct(int correct, int answered) {
    if (answered == 0) {
      return null;
    }
    return (correct * 100.0) / answered;
  }

  static int amPmIndex(int hour) {
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
}
