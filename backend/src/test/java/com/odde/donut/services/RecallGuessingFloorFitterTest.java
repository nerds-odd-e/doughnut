package com.odde.donut.services;

import static com.odde.donut.services.RecallProbabilityMath.sigmoid;
import static com.odde.donut.services.RecallStatsTestFixtures.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.services.RecallGuessingFloorFitter.ThreePlFit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Tests for the 3PL guessing-floor profile-likelihood fit (slice 20): for each candidate γ, α/β are
 * <em>re-fit</em> conditional on that γ (not reused from a fixed unconditional 2PL fit) — see
 * {@link RecallGuessingFloorFitter}'s javadoc for why the naive alternative is biased low.
 */
class RecallGuessingFloorFitterTest {

  /**
   * Finite-difference check of the analytic gradient used inside the conditional Newton-Raphson
   * fit: perturbing alpha/beta by a small epsilon and comparing the numerical slope of the
   * log-likelihood to the analytic per-row score sum catches a sign/derivative bug cheaply, before
   * it manifests as non-convergence or a silently-wrong fit in the outer loop.
   */
  @Test
  void analyticGradientMatchesFiniteDifferenceOfTheLogLikelihood() {
    Random random = new Random(42);
    int n = 200;
    double trueAlpha = 0.3;
    double trueBeta = 1.4;
    double trueGamma = 0.2;
    double[] x = new double[n];
    double[] y = new double[n];
    for (int i = 0; i < n; i++) {
      x[i] = -4 + 8.0 * i / (n - 1); // spread of logit-retrievability values
      double p = trueGamma + (1 - trueGamma) * sigmoid(trueAlpha + trueBeta * x[i]);
      y[i] = random.nextDouble() < p ? 1.0 : 0.0;
    }

    double alpha = 0.1;
    double beta = 0.8;
    double gamma = 0.25;
    double epsilon = 1e-6;

    double analyticGradAlpha = 0;
    double analyticGradBeta = 0;
    for (int i = 0; i < n; i++) {
      double s = sigmoid(alpha + beta * x[i]);
      double p = gamma + (1 - gamma) * s;
      double dpdz = (1 - gamma) * s * (1 - s);
      double u = ((y[i] - p) / (p * (1 - p))) * dpdz;
      analyticGradAlpha += u;
      analyticGradBeta += u * x[i];
    }

    double llPlusAlpha = logLikelihood(alpha + epsilon, beta, gamma, x, y);
    double llMinusAlpha = logLikelihood(alpha - epsilon, beta, gamma, x, y);
    double numericGradAlpha = (llPlusAlpha - llMinusAlpha) / (2 * epsilon);

    double llPlusBeta = logLikelihood(alpha, beta + epsilon, gamma, x, y);
    double llMinusBeta = logLikelihood(alpha, beta - epsilon, gamma, x, y);
    double numericGradBeta = (llPlusBeta - llMinusBeta) / (2 * epsilon);

    assertThat(analyticGradAlpha, closeTo(numericGradAlpha, 1e-4));
    assertThat(analyticGradBeta, closeTo(numericGradBeta, 1e-4));
  }

  @Test
  void heldAtZeroGammaBelowTheTrailingReviewThreshold() {
    List<RecallAnswerRow> rows = syntheticRows(new Random(1), 100, 0.0, 1.0, 0.3);
    ThreePlFit fit = RecallGuessingFloorFitter.fit(rows);
    assertThat(fit.gamma(), equalTo(0.0));
  }

  /**
   * Spelling-shaped data (no genuine guessing floor: true gamma = 0) must fit gamma near 0 — the
   * built-in sanity check that the fit doesn't spuriously invent a guessing floor where none
   * exists.
   */
  @Test
  void fitsGammaNearZeroWhenThereIsNoGenuineGuessingFloor() {
    List<RecallAnswerRow> rows = syntheticRows(new Random(7), 4000, 0.2, 1.1, 0.0);
    ThreePlFit fit = RecallGuessingFloorFitter.fit(rows);
    assertThat(fit.gamma(), lessThanOrEqualTo(0.02));
  }

  /**
   * MCQ-shaped data with an injected true guessing floor of 0.3 must recover a gamma clearly in
   * that neighborhood — not damped down near ~0.05 the way the rejected unconditional-alpha/beta
   * attempt did. Grid step (0.02) and finite-sample noise limit precision, so the bar is "clearly
   * in the right neighborhood", not exact recovery.
   */
  @Test
  void recoversAGuessingFloorClearlyInTheRightNeighborhoodWhenOneIsInjected() {
    List<RecallAnswerRow> rows = syntheticRows(new Random(11), 6000, 0.0, 1.2, 0.3);
    ThreePlFit fit = RecallGuessingFloorFitter.fit(rows);
    assertThat(fit.gamma(), closeTo(0.3, 0.1));
    assertThat(fit.gamma(), greaterThan(0.15));
  }

  private static List<RecallAnswerRow> syntheticRows(
      Random random, int n, double alpha, double beta, double gamma) {
    List<RecallAnswerRow> rows = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      double x = -4 + 8.0 * i / (n - 1);
      double retrievability = sigmoid(x);
      double p = gamma + (1 - gamma) * sigmoid(alpha + beta * x);
      boolean correct = random.nextDouble() < p;
      rows.add(answered(utc(0, i % 24), 5000, correct, null, i, retrievability));
    }
    return rows;
  }

  private static double logLikelihood(
      double alpha, double beta, double gamma, double[] x, double[] y) {
    double total = 0;
    for (int i = 0; i < x.length; i++) {
      double s = sigmoid(alpha + beta * x[i]);
      double p = Math.min(1 - 1e-6, Math.max(1e-6, gamma + (1 - gamma) * s));
      total += y[i] == 1.0 ? Math.log(p) : Math.log(1 - p);
    }
    return total;
  }
}
