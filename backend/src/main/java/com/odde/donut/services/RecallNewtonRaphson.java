package com.odde.donut.services;

/**
 * Shared Newton-Raphson scaffold for the two-parameter (α, β) logistic fits used by {@link
 * RecallCalibrationFitter} (analytic Fisher Hessian) and {@link RecallGuessingFloorFitter} (BHHH
 * approximation). Owns only the iteration loop, 2×2 Newton step, backtracking line search, and
 * bailout/convergence checks — callers supply the per-iteration gradient, curvature, and
 * log-likelihood, which is where the 2PL and 3PL scoring math actually differs.
 *
 * <p>A full undamped Newton step can overshoot on early iterations, so each step is halved until
 * the log-likelihood actually improves. {@code converged=false} means the curvature was degenerate,
 * a step was NaN/Infinite, or line search found no improving step; {@code converged=true} means the
 * parameter change fell under {@link #CONVERGENCE_TOLERANCE} or {@link #MAX_ITERATIONS} was reached
 * (callers treat max-iter as success and keep the last parameters).
 */
final class RecallNewtonRaphson {
  private static final int MAX_ITERATIONS = 25;
  private static final double CONVERGENCE_TOLERANCE = 1e-6;
  private static final int MAX_LINE_SEARCH_HALVINGS = 30;
  private static final double SINGULARITY_THRESHOLD = 1e-9;

  private RecallNewtonRaphson() {}

  /**
   * Per-iteration gradient of the log-likelihood and the 2×2 curvature used for the Newton step
   * (analytic Fisher information for 2PL; BHHH outer-product approximation for 3PL). Treated as a
   * positive-definite approximation of {@code -H}; the step solves {@code curvature · Δ =
   * gradient}.
   */
  record Score(double gradAlpha, double gradBeta, double hAA, double hAB, double hBB) {}

  record Fit(double alpha, double beta, double logLikelihood, boolean converged) {}

  @FunctionalInterface
  interface ScoreFn {
    Score score(double alpha, double beta);
  }

  @FunctionalInterface
  interface LogLikelihoodFn {
    double logLikelihood(double alpha, double beta);
  }

  static Fit maximize(double alpha, double beta, ScoreFn scoreFn, LogLikelihoodFn logLikelihoodFn) {
    double logLikelihood = logLikelihoodFn.logLikelihood(alpha, beta);
    for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
      Score score = scoreFn.score(alpha, beta);
      double determinant = score.hAA() * score.hBB() - score.hAB() * score.hAB();
      if (determinant < SINGULARITY_THRESHOLD) {
        return new Fit(alpha, beta, logLikelihood, false);
      }
      double deltaAlpha =
          (score.hBB() * score.gradAlpha() - score.hAB() * score.gradBeta()) / determinant;
      double deltaBeta =
          (score.hAA() * score.gradBeta() - score.hAB() * score.gradAlpha()) / determinant;
      if (Double.isNaN(deltaAlpha) || Double.isNaN(deltaBeta)) {
        return new Fit(alpha, beta, logLikelihood, false);
      }
      double step = 1.0;
      double newAlpha = alpha;
      double newBeta = beta;
      double newLogLikelihood = Double.NEGATIVE_INFINITY;
      for (int halving = 0; halving < MAX_LINE_SEARCH_HALVINGS; halving++) {
        newAlpha = alpha + step * deltaAlpha;
        newBeta = beta + step * deltaBeta;
        newLogLikelihood = logLikelihoodFn.logLikelihood(newAlpha, newBeta);
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
        return new Fit(alpha, beta, logLikelihood, false);
      }
      boolean converged =
          Math.abs(newAlpha - alpha) < CONVERGENCE_TOLERANCE
              && Math.abs(newBeta - beta) < CONVERGENCE_TOLERANCE;
      alpha = newAlpha;
      beta = newBeta;
      logLikelihood = newLogLikelihood;
      if (converged) {
        return new Fit(alpha, beta, logLikelihood, true);
      }
    }
    return new Fit(alpha, beta, logLikelihood, true);
  }
}
