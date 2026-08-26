package com.odde.donut.controllers.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecallStatsDTO {
  private List<DayCount> calendar;
  private List<DayAvgResponseTime> trend;
  private List<DayRetention> retentionTrend;
  private AmPmResponseTime amPm;
  private int[][] weekdayHourCounts;
  private int[][] weekdayHourCorrect;
  private HeadlineStats totals;
  private PaceStats pace;

  @Data
  @AllArgsConstructor
  public static class DayCount {
    private String date;
    private int count;
  }

  @Data
  @AllArgsConstructor
  public static class DayAvgResponseTime {
    private String date;
    private Long avgMs;
    private Integer sampleSize;
  }

  @Data
  @AllArgsConstructor
  public static class DayRetention {
    private String date;
    private Double retentionPct;
  }

  @Data
  @AllArgsConstructor
  public static class AmPmResponseTime {
    private Long morningMs;
    private Integer morningSamples;
    private Long afternoonMs;
    private Integer afternoonSamples;
    private Long eveningMs;
    private Integer eveningSamples;
    private Long nightMs;
    private Integer nightSamples;
  }

  @Data
  @AllArgsConstructor
  public static class PaceStats {
    private Double pctVsUsual;
    private Integer sampleSize;
    private Integer totalAnsweredToday;
    private Double confidence;
  }

  @Data
  @AllArgsConstructor
  public static class HeadlineStats {
    private int totalReviewsAllTime;
    private int totalReviews365;
    private int reviewsToday;
    private Double retentionPct365;
    private int currentStreak;
    private int longestStreak;
    private long totalTimeSpentMs;
    private Integer bestHour;
    private Double bestHourRetentionPct;
    private Integer worstHour;
    private Double worstHourRetentionPct;
  }
}
