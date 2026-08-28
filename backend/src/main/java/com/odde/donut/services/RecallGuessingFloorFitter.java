package com.odde.donut.services;

import static com.odde.donut.services.RecallProbabilityMath.clamp;
import static com.odde.donut.services.RecallProbabilityMath.logit;
import static com.odde.donut.services.RecallProbabilityMath.sigmoid;

import com.odde.donut.services.RecallCalibrationFitter.CalibrationFit;
import java.util.List;

/**
 * 3PL guessing-floor fit: {@code p̂(α, β, γ) = γ + (1 − γ) · σ(α + β · logit(retrievability))}, fit
 * per question type by profile likelihood over γ. {@link RecallCalibrationFitter} fits α/β by
 * Newton-Raphson for a <em>given</em> γ (the {@link #MIN_TRAILING_REVIEWS} branch below reuses it
 * directly, at γ=0); this class grid-searches γ over {@code [0, 0.5]} (step 0.02) and, for each
 * candidate, re-fits α/β from scratch conditional on that γ (warm-started from the previous grid
 * point's converged values — a continuation method) before comparing log-likelihoods.
 *
 * <p>A naive alternative — fit α/β once as an unconditional 2PL, then grid-search only γ against
 * that fixed fit — is numerically simpler but substantially biased low: it never lets α/β absorb
 * what a genuine guessing floor would otherwise explain, so γ ends up soaking up only the residual
 * the fixed slope couldn't. The conditional refit here is what makes the estimate correct, at the
 * cost of one Newton-Raphson run per grid point instead of one total.
 *
 * <p>Held at exactly γ=0 below {@link #MIN_TRAILING_REVIEWS} qualifying trailing rows for a
 * question type — a second, independent threshold from {@code
 * RecallCalibrationFitter#MIN_CALIBRATION_SAMPLES}, which still gates the α/β fit itself. A
 * guessing floor needs far more data to identify than a slope: it only bites in the
 * low-retrievability tail, so most of the trailing window carries no information about it.
 */
final class RecallGuessingFloorFitter {
  static final int MIN_TRAILING_REVIEWS = 300;
  private static final double GAMMA_MIN = 0.0;
  private static final double GAMMA_MAX = 0.5;
  private static final double GAMMA_STEP = 0.02;

  private RecallGuessingFloorFitter() {}

  record ThreePlFit(double alpha, double beta, double gamma) {
    static final ThreePlFit IDENTITY = new ThreePlFit(0.0, 1.0, 0.0);

    /** Maps a raw retrievability to the recalibrated, guessing-floor-adjusted probability. */
    double recalibrate(double retrievability) {
      if (this == IDENTITY) {
        return retrievability;
      }
      double s = sigmoid(alpha + beta * logit(clamp(retrievability)));
      return gamma + (1 - gamma) * s;
    }
  }

  /**
   * Fits the 3PL model for one question type's trailing qualifying rows. Callers pass only rows
   * that already qualify (same exclusions as {@link RecallCalibrationFitter}) and that already
   * belong to a single {@code RecallPrompt.questionType} — see {@link RecallAccuracyAggregator}.
   */
  static ThreePlFit fit(List<RecallAnswerRow> qualifyingRows) {
    int n = qualifyingRows.size();
    if (n < MIN_TRAILING_REVIEWS) {
      CalibrationFit twoPl = RecallCalibrationFitter.fit(qualifyingRows);
      return twoPl == CalibrationFit.IDENTITY
          ? ThreePlFit.IDENTITY
          : new ThreePlFit(twoPl.alpha(), twoPl.beta(), 0.0);
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
      return ThreePlFit.IDENTITY;
    }
    return gridSearchGamma(x, y);
  }

  /**
   * Profile likelihood over γ: for each grid point, conditionally refit α/β (warm-started from the
   * previous grid point) and keep the γ whose refit log-likelihood is highest. A grid point whose
   * conditional refit fails to converge (singular Hessian, non-improving step) is simply excluded
   * from the max, and does not advance the warm start.
   */
  private static ThreePlFit gridSearchGamma(double[] x, double[] y) {
    double warmAlpha = 0.0;
    double warmBeta = 0.0;
    double bestGamma = 0.0;
    double bestAlpha = 0.0;
    double bestBeta = 1.0;
    double bestLogLikelihood = Double.NEGATIVE_INFINITY;
    int steps = (int) Math.round((GAMMA_MAX - GAMMA_MIN) / GAMMA_STEP);
    for (int i = 0; i <= steps; i++) {
      double gamma = Math.min(GAMMA_MAX, GAMMA_MIN + i * GAMMA_STEP);
      RecallNewtonRaphson.Fit fit =
          RecallNewtonRaphson.maximize(
              warmAlpha,
              warmBeta,
              (alpha, beta) -> score(alpha, beta, gamma, x, y),
              (alpha, beta) -> logLikelihood(alpha, beta, gamma, x, y));
      if (!fit.converged()) {
        continue;
      }
      warmAlpha = fit.alpha();
      warmBeta = fit.beta();
      if (fit.logLikelihood() > bestLogLikelihood) {
        bestLogLikelihood = fit.logLikelihood();
        bestGamma = gamma;
        bestAlpha = fit.alpha();
        bestBeta = fit.beta();
      }
    }
    if (bestLogLikelihood == Double.NEGATIVE_INFINITY) {
      return ThreePlFit.IDENTITY;
    }
    return new ThreePlFit(bestAlpha, bestBeta, bestGamma);
  }

  /**
   * Outer-product-of-gradients (BHHH) curvature for a <em>fixed</em> gamma, rather than the
   * analytic second derivative through gamma's non-canonical link: simpler and safer to get the
   * sign right, at the cost of possibly more iterations. Validated against a finite-difference
   * gradient check in {@code RecallGuessingFloorFitterTest}.
   *
   * <p>Per-row score {@code u_i = ((y_i − p_i) / (p_i·(1−p_i))) · dp_i/dz_i}, where {@code
   * dp_i/dz_i = (1−γ)·σ(z_i)·(1−σ(z_i))}; gradient {@code g = Σ u_i·[1, x_i]}; BHHH curvature
   * {@code M = Σ u_i²·[1,x_i][1,x_i]ᵀ ≈ -H}, so the Newton step solves {@code M·Δ = g}.
   */
  private static RecallNewtonRaphson.Score score(
      double alpha, double beta, double gamma, double[] x, double[] y) {
    double gradAlpha = 0;
    double gradBeta = 0;
    double hAA = 0;
    double hAB = 0;
    double hBB = 0;
    for (int i = 0; i < x.length; i++) {
      double s = sigmoid(alpha + beta * x[i]);
      double p = clamp(gamma + (1 - gamma) * s);
      double dpdz = (1 - gamma) * s * (1 - s);
      double u = ((y[i] - p) / (p * (1 - p))) * dpdz;
      gradAlpha += u;
      gradBeta += u * x[i];
      hAA += u * u;
      hAB += u * u * x[i];
      hBB += u * u * x[i] * x[i];
    }
    return new RecallNewtonRaphson.Score(gradAlpha, gradBeta, hAA, hAB, hBB);
  }

  /** Numerically stable Bernoulli log-likelihood under the current alpha/beta/gamma. */
  private static double logLikelihood(
      double alpha, double beta, double gamma, double[] x, double[] y) {
    double total = 0;
    for (int i = 0; i < x.length; i++) {
      double s = sigmoid(alpha + beta * x[i]);
      double p = clamp(gamma + (1 - gamma) * s);
      total += y[i] == 1.0 ? Math.log(p) : Math.log(1 - p);
    }
    return total;
  }
}
