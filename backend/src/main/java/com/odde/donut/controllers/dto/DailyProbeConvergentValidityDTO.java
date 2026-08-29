package com.odde.donut.controllers.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Internal diagnostic (plan {@code 008-probe-convergent-analyses}) reporting, for each of four
 * matched probe-metric/recall-component pairs, the raw Pearson correlation across the current
 * user's trailing morning history. Not user-facing — see {@code RecallProbeConvergentValidity} for
 * how the pairs are matched and gated.
 */
@Data
@AllArgsConstructor
public class DailyProbeConvergentValidityDTO {
  private List<PairValidity> pairs;

  @Data
  @AllArgsConstructor
  public static class PairValidity {
    private String pair;
    private Integer pairCount;
    private Double rawCorrelation;
  }
}
