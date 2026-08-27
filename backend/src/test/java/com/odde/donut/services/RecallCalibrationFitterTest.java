package com.odde.donut.services;

import static com.odde.donut.services.RecallStatsTestFixtures.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.services.RecallCalibrationFitter.CalibrationFit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Platt-scaling-style logistic recalibration fit ({@code p̂ = sigmoid(alpha + beta ·
 * logit(retrievability))}) that {@link RecallAccuracyAggregator} refits on every request from the
 * trailing 180-day window (slice 19).
 */
class RecallCalibrationFitterTest {
  @Test
  void fallsBackToIdentityWhenThereAreTooFewQualifyingRows() {
    List<RecallAnswerRow> rows = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      rows.add(answered(utc(0, i), 5000, i % 2 == 0, null, i, 0.7));
    }
    CalibrationFit fit = RecallCalibrationFitter.fit(rows);
    // Identity: recalibrate returns the raw value unchanged, exactly.
    assertThat(fit.recalibrate(0.7), equalTo(0.7));
    assertThat(fit.recalibrate(0.0), equalTo(0.0));
    assertThat(fit.recalibrate(1.0), equalTo(1.0));
  }

  @Test
  void fallsBackToIdentityWhenEveryTrailingOutcomeIsTheSame() {
    List<RecallAnswerRow> rows = new ArrayList<>();
    for (int i = 0; i < 80; i++) {
      // Every row correct: no outcome variance, a slope isn't identifiable.
      rows.add(answered(utc(0, i % 24), 5000, true, null, i, 0.5 + (i % 5) * 0.1));
    }
    CalibrationFit fit = RecallCalibrationFitter.fit(rows);
    assertThat(fit.recalibrate(0.5), equalTo(0.5));
  }

  @Test
  void fallsBackToIdentityWhenRetrievabilityHasNoVarianceAcrossRows() {
    List<RecallAnswerRow> rows = new ArrayList<>();
    for (int i = 0; i < 80; i++) {
      // Same retrievability on every row (mixed outcomes) -> alpha/beta not jointly
      // identifiable (singular Fisher information).
      rows.add(answered(utc(0, i % 24), 5000, i % 2 == 0, null, i, 0.6));
    }
    CalibrationFit fit = RecallCalibrationFitter.fit(rows);
    assertThat(fit.recalibrate(0.6), equalTo(0.6));
  }

  @Test
  void fitsASaturatedTwoGroupCalibrationExactlyToTheEmpiricalProportions() {
    List<RecallAnswerRow> rows = new ArrayList<>();
    // Group A: retrievability 0.9 (FSRS is confident), but the learner is only actually right
    // half the time on these items -> the scheduler is systematically overconfident here.
    for (int i = 0; i < 40; i++) {
      rows.add(answered(utc(0, i % 24), 5000, i < 20, null, i, 0.9));
    }
    // Group B: retrievability 0.5, but the learner is actually right 80% of the time -> the
    // scheduler is systematically underconfident here.
    for (int i = 40; i < 80; i++) {
      rows.add(answered(utc(0, i % 24), 5000, i < 72, null, i, 0.5));
    }
    CalibrationFit fit = RecallCalibrationFitter.fit(rows);
    // With exactly two distinct retrievability values, a 2-parameter logistic fit is saturated:
    // the recalibrated probability at each group's retrievability converges to that group's
    // empirical proportion, regardless of what the raw retrievability said.
    assertThat(fit.recalibrate(0.9), closeTo(0.5, 0.01));
    assertThat(fit.recalibrate(0.5), closeTo(0.8, 0.01));
  }
}
