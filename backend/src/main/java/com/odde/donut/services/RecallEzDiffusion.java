package com.odde.donut.services;

import com.odde.donut.entities.QuestionType;
import com.odde.donut.utils.TimestampOperations;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Trailing three-local-morning EZ-diffusion decomposition of MCQ recall trials (plan {@code
 * 008-probe-convergent-analyses}, slice 3). Pools every qualifying trial across the rolling
 * three-morning window into a single fit — it deliberately does not pick a single morning to fit,
 * even one with enough trials on its own, since RT variance over one morning is noisy. Delegates
 * the closed-form algebra to {@link EzDiffusion}.
 *
 * <p>Exclusion set is a simplified version of {@link RecallPaceAggregator}'s: this reuses its fixed
 * {@link RecallPaceAggregator#ABSOLUTE_FLOOR_MS absolute floor} and {@link
 * RecallPaceAggregator#HARD_DROP_MS hard-drop ceiling} directly, but not the per-item
 * EWMA-baseline-relative floor — that machinery is a chronological, per-item walk over all-time
 * history, and wiring it into this aggregator too was heavier than this slice's budget. Callers
 * pass in reviews only (see {@code RecallStatsService#reviewsOnly}), matching {@link
 * RecallProbeConvergentValidity}'s convention.
 */
final class RecallEzDiffusion {
  /** {@code today.minusDays(2)}..{@code today} inclusive = three calendar mornings. */
  private static final int TRAILING_WINDOW_DAYS_AGO = 2;

  /** EZ needs 30-50 trials; 30 is the floor to emit a fit. */
  private static final int MIN_TRIALS_FOR_FIT = 30;

  private RecallEzDiffusion() {}

  record Result(
      Double driftRate,
      Double boundarySeparation,
      Double nondecisionTimeMs,
      int trialCount,
      int morningCount) {}

  static Result compute(List<RecallAnswerRow> allTimeReviews, LocalDate today, ZoneId zoneId) {
    LocalDate windowStart = today.minusDays(TRAILING_WINDOW_DAYS_AGO);
    List<Double> rtSeconds = new ArrayList<>();
    Set<LocalDate> qualifyingMornings = new HashSet<>();
    int correctCount = 0;

    for (RecallAnswerRow r : allTimeReviews) {
      if (r.questionType() != QuestionType.MCQ || r.answerCreatedAt() == null) {
        continue;
      }
      LocalDate rowDate =
          TimestampOperations.getZonedDateTime(r.answerCreatedAt(), zoneId).toLocalDate();
      if (rowDate.isBefore(windowStart) || rowDate.isAfter(today)) {
        continue;
      }
      Optional<Long> rt = r.rawElapsedMs();
      if (rt.isEmpty()) {
        continue;
      }
      long onTaskMs = rt.get();
      if (onTaskMs < RecallPaceAggregator.ABSOLUTE_FLOOR_MS
          || onTaskMs >= RecallPaceAggregator.HARD_DROP_MS) {
        continue;
      }
      qualifyingMornings.add(rowDate);
      rtSeconds.add(onTaskMs / 1000.0);
      if (r.correct()) {
        correctCount++;
      }
    }

    int trialCount = rtSeconds.size();
    int morningCount = qualifyingMornings.size();
    if (trialCount < MIN_TRIALS_FOR_FIT) {
      return new Result(null, null, null, trialCount, morningCount);
    }

    double p = (double) correctCount / trialCount;
    double meanRt = rtSeconds.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
    double varianceRt =
        rtSeconds.stream().mapToDouble(v -> (v - meanRt) * (v - meanRt)).sum() / (trialCount - 1);

    EzDiffusion.Parameters params = EzDiffusion.recover(p, meanRt, varianceRt, trialCount);
    Double nondecisionTimeMs =
        params.nondecisionTime() == null ? null : params.nondecisionTime() * 1000;
    return new Result(
        params.driftRate(),
        params.boundarySeparation(),
        nondecisionTimeMs,
        trialCount,
        morningCount);
  }
}
