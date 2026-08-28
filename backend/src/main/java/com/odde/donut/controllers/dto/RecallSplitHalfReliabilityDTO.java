package com.odde.donut.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Internal diagnostic (slice 21.4) reporting the morning cognitive index's split-half reliability
 * across a trailing window of the current user's historical mornings. Not user-facing, and
 * deliberately not part of {@link RecallStatsDTO} — see {@code RecallSplitHalfReliability} for how
 * the two numbers are derived and why both are reported.
 */
@Data
@AllArgsConstructor
public class RecallSplitHalfReliabilityDTO {
  private Integer pairCount;
  private Double rawCorrelation;
  private Double spearmanBrownCorrelation;

  // TEMP-DEBUG (slice 21.4 prod investigation, remove once cause of prod pairCount=0 is found):
  private Integer tempDebugCandidateDayCount;
  private Integer tempDebugAccuracyNullCount;
  private Integer tempDebugPaceStatsNullCount;
  private Integer tempDebugDayBaselineNullCount;
}
