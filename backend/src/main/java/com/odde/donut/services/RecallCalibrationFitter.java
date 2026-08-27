package com.odde.donut.services;

import java.util.List;

/**
 * Platt-scaling-style logistic recalibration: fits {@code p̂_recalibrated = sigmoid(alpha + beta ·
 * logit(retrievability))} from a trailing window of {@code (retrievability, outcome)} pairs, so a
 * learner whose FSRS profile is systematically over- or under-confident gets an accuracy readout
 * corrected for that bias rather than one that perpetually reads "worse/better than expected" for a
 * reason that has nothing to do with today.
 *
 * <p>Fit via Newton-Raphson/IRLS for the two parameters (small, well-bounded, no external numerics
 * dependency — mirrors {@code Fsrs.java}'s style of self-contained numeric code). Falls back to the
 * identity mapping ({@code alpha=0, beta=1}, i.e. raw retrievability unrecalibrated) whenever there
 * isn't enough data to fit reliably: too few qualifying rows, no variance in the outcome, or a
 * degenerate/non-converging fit. Never crashes, never returns nonsense.
 */
final class RecallCalibrationFitter {
  /**
   * Below this many qualifying trailing rows, a 2-parameter logistic fit is too noisy to trust (few
   * dozen Bernoulli observations barely constrain a slope) — mirrors {@code
   * RecallPaceAggregator#MIN_BASELINE_DAYS}'s role of gating a trailing-window statistic on having
   * enough data before it's allowed to speak.
   */
  private static final int MIN_CALIBRATION_SAMPLES = 50;

  private static final int MAX_ITERATIONS = 25;
  private static final double CONVERGENCE_TOLERANCE = 1e-6;
  private static final double PROBABILITY_EPSILON = 1e-6;

  private RecallCalibrationFitter() {}

  record CalibrationFit(double alpha, double beta) {
    static final CalibrationFit IDENTITY = new CalibrationFit(0.0, 1.0);

    /**
     * Maps a raw retrievability to the recalibrated probability. The identity fit returns the raw
     * value unchanged (not a logit/sigmoid round-trip) so it is exact even at retrievability 0 or
     * 1, where a round-trip through {@link #clamp} would otherwise perturb it.
     */
    double recalibrate(double retrievability) {
      if (this == IDENTITY) {
        return retrievability;
      }
      return sigmoid(alpha + beta * logit(clamp(retrievability)));
    }
  }

  /**
   * Fits alpha/beta by Newton-Raphson on the logistic log-likelihood. Callers pass only rows that
   * already qualify for calibration (trailing window, non-null retrievability, not an
   * implausibly-fast mistap) — see {@link RecallAccuracyAggregator}.
   */
  static CalibrationFit fit(List<RecallAnswerRow> qualifyingRows) {
    int n = qualifyingRows.size();
    if (n < MIN_CALIBRATION_SAMPLES) {
      return CalibrationFit.IDENTITY;
    }
    double[] x = new double[n];
    double[] y = new double[n];
    boolean sawCorrect = false;
    boolean sawIncorrect = false;
    for (int i = 0; i < n; i++) {
      RecallAnswerRow r = qualifyingRows.get(i);
      x[i] = logit(clamp(r.retrievability()));
      boolean correct = r.correct();
      y[i] = correct ? 1.0 : 0.0;
      sawCorrect = sawCorrect || correct;
      sawIncorrect = sawIncorrect || !correct;
    }
    if (!sawCorrect || !sawIncorrect) {
      // No outcome variance (e.g. every trailing row correct) — there is nothing to recalibrate
      // against, and a slope isn't identifiable.
      return CalibrationFit.IDENTITY;
    }
    return newtonRaphson(x, y);
  }

  /**
   * Newton-Raphson with backtracking line search. A full undamped Newton step can overshoot badly
   * on the first few iterations here (the two-parameter fit is well-conditioned but the starting
   * point is arbitrary), so each step is halved until the log-likelihood actually improves — the
   * standard fix that restores Newton's guaranteed convergence for a concave objective like
   * logistic log-likelihood. Starts at {@code alpha=0, beta=0} (the "retrievability carries no
   * information" point), not {@code beta=1} (the identity mapping) — starting exactly at the
   * no-slope point turned out to make the first step's direction unstable for this problem.
   */
  private static CalibrationFit newtonRaphson(double[] x, double[] y) {
    double alpha = 0.0;
    double beta = 0.0;
    double logLikelihood = logLikelihood(alpha, beta, x, y);
    for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
      double gradAlpha = 0;
      double gradBeta = 0;
      double hAA = 0;
      double hAB = 0;
      double hBB = 0;
      for (int i = 0; i < x.length; i++) {
        double p = sigmoid(alpha + beta * x[i]);
        double w = p * (1 - p);
        double residual = y[i] - p;
        gradAlpha += residual;
        gradBeta += residual * x[i];
        hAA += w;
        hAB += w * x[i];
        hBB += w * x[i] * x[i];
      }
      double determinant = hAA * hBB - hAB * hAB;
      if (determinant < 1e-9) {
        // Near-singular Fisher information (e.g. every row shares the same retrievability, so
        // alpha and beta are not jointly identifiable) — bail out rather than blow up.
        return CalibrationFit.IDENTITY;
      }
      double deltaAlpha = (hBB * gradAlpha - hAB * gradBeta) / determinant;
      double deltaBeta = (hAA * gradBeta - hAB * gradAlpha) / determinant;
      if (Double.isNaN(deltaAlpha) || Double.isNaN(deltaBeta)) {
        return CalibrationFit.IDENTITY;
      }
      double step = 1.0;
      double newAlpha = alpha;
      double newBeta = beta;
      double newLogLikelihood = Double.NEGATIVE_INFINITY;
      for (int halving = 0; halving < 30; halving++) {
        newAlpha = alpha + step * deltaAlpha;
        newBeta = beta + step * deltaBeta;
        newLogLikelihood = logLikelihood(newAlpha, newBeta, x, y);
        if (newLogLikelihood >= logLikelihood) {
          break;
        }
        step /= 2;
      }
      if (Double.isNaN(newAlpha)
          || Double.isNaN(newBeta)
          || Double.isInfinite(newAlpha)
          || Double.isInfinite(newBeta)
          || newLogLikelihood < logLikelihood) {
        return CalibrationFit.IDENTITY;
      }
      boolean converged =
          Math.abs(newAlpha - alpha) < CONVERGENCE_TOLERANCE
              && Math.abs(newBeta - beta) < CONVERGENCE_TOLERANCE;
      alpha = newAlpha;
      beta = newBeta;
      logLikelihood = newLogLikelihood;
      if (converged) {
        return new CalibrationFit(alpha, beta);
      }
    }
    return new CalibrationFit(alpha, beta);
  }

  /** Numerically stable Bernoulli log-likelihood under the current alpha/beta. */
  private static double logLikelihood(double alpha, double beta, double[] x, double[] y) {
    double total = 0;
    for (int i = 0; i < x.length; i++) {
      double z = alpha + beta * x[i];
      // log(sigmoid(z)) = -log1p(exp(-z)); log(1 - sigmoid(z)) = -log1p(exp(z))
      total += y[i] == 1.0 ? -Math.log1p(Math.exp(-z)) : -Math.log1p(Math.exp(z));
    }
    return total;
  }

  private static double clamp(double p) {
    return Math.min(1 - PROBABILITY_EPSILON, Math.max(PROBABILITY_EPSILON, p));
  }

  private static double logit(double p) {
    return Math.log(p / (1 - p));
  }

  private static double sigmoid(double z) {
    return 1.0 / (1.0 + Math.exp(-z));
  }
}
