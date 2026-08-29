package com.odde.donut.services;

/**
 * Package-local pure closed-form implementation of the EZ-diffusion model (Wagenmakers, van der
 * Maas &amp; Grasman, 2007, "An EZ-diffusion model for response time and accuracy", Psychonomic
 * Bulletin &amp; Review 14, 3-22), recovering drift rate ({@code v}), boundary separation ({@code
 * a}), and nondecision time ({@code Ter}) from proportion correct and the mean/variance of response
 * time. Implements the paper's Equations 5, 7, 8, 9 and its Appendix R function {@code get.vaTer}
 * verbatim (verified against the paper's own worked numerical example: v=0.1, a=0.14, Ter=0.300,
 * s=0.1 forward-predicts MRT=0.723, VRT=0.112, Pc=.802 — reproduced exactly by {@link #recover}'s
 * inverse computation).
 *
 * <p>Consumed only by slice 3 (plan {@code 008-probe-convergent-analyses}) so the algebra lives in
 * one place rather than being invented inside a controller.
 */
final class EzDiffusion {
  /** Conventional diffusion coefficient used throughout the EZ-diffusion literature. */
  private static final double S = 0.1;

  private EzDiffusion() {}

  /**
   * @param driftRate {@code v}: quality of information. {@code null} when undefined.
   * @param boundarySeparation {@code a}: response conservativeness. {@code null} when undefined.
   * @param nondecisionTime {@code Ter}, in seconds. {@code null} when undefined.
   */
  record Parameters(Double driftRate, Double boundarySeparation, Double nondecisionTime) {
    private static final Parameters UNDEFINED = new Parameters(null, null, null);
  }

  /**
   * @param p proportion correct over the responded trials.
   * @param meanRtSeconds mean response time in seconds, over the same responded trials (correct and
   *     error).
   * @param varianceRtSeconds sample (n−1) variance of response time in seconds, over the same
   *     responded trials.
   * @param n number of responded trials.
   */
  static Parameters recover(double p, double meanRtSeconds, double varianceRtSeconds, int n) {
    if (n < 2 || varianceRtSeconds <= 0) {
      return Parameters.UNDEFINED;
    }

    double correctedP = edgeCorrect(p, n);
    if (correctedP == 0.5) {
      return recoverAtChance(meanRtSeconds, varianceRtSeconds);
    }

    double s2 = S * S;
    double l = Math.log(correctedP / (1 - correctedP));
    double x =
        l * (l * correctedP * correctedP - l * correctedP + correctedP - 0.5) / varianceRtSeconds;
    double v = Math.signum(correctedP - 0.5) * S * Math.pow(x, 0.25);
    double a = s2 * l / v;
    double y = -v * a / s2;
    double meanDecisionTime = (a / (2 * v)) * (1 - Math.exp(y)) / (1 + Math.exp(y));
    double ter = meanRtSeconds - meanDecisionTime;
    return new Parameters(v, a, ter);
  }

  /**
   * {@code P = 0.5} special case (the paper's {@code L = logit(0.5) = 0} boundary): the general
   * equations above divide by {@code v}, which is also exactly zero at this point (drift is zero by
   * symmetry when accuracy is at chance), so the closed form here is the {@code L → 0} limit rather
   * than a division by {@code logit(0.5)}.
   *
   * <ul>
   *   <li>{@code v = 0} exactly, by symmetry (the general formula's {@code x} already has a factor
   *       of {@code L}, so it evaluates to exactly zero at {@code L = 0} — no division occurs
   *       computing {@code v} itself).
   *   <li>{@code a} follows by inverting the paper's own stated {@code v = 0} special case of the
   *       forward variance equation, {@code VRT = a⁴ / (24s⁴)} (paper text, directly below Equation
   *       6): {@code a = (24 · s⁴ · VRT)^(1/4)}.
   *   <li>Mean decision time follows the standard driftless-Wiener first-passage-time result for a
   *       start point at {@code a/2} equidistant from both boundaries: {@code MDT = a² / (4s²)}
   *       (also derivable as the {@code L → 0} limit of Equation 9 substituted with the {@code a}
   *       above; cross-checked numerically in the test against the general formula evaluated at a
   *       tiny nonzero {@code L}).
   * </ul>
   */
  private static Parameters recoverAtChance(double meanRtSeconds, double varianceRtSeconds) {
    double s2 = S * S;
    double a = Math.pow(24 * s2 * s2 * varianceRtSeconds, 0.25);
    double meanDecisionTime = (a * a) / (4 * s2);
    double ter = meanRtSeconds - meanDecisionTime;
    return new Parameters(0.0, a, ter);
  }

  /**
   * Standard edge correction (paper text, describing the {@code Pc = 1} case; applied symmetrically
   * to {@code Pc = 0}, and matching the Appendix R code's guard for both extremes): a value
   * corresponding to one half of an error, so that {@code logit} stays finite.
   */
  private static double edgeCorrect(double p, int n) {
    if (p == 0) {
      return 1.0 / (2 * n);
    }
    if (p == 1) {
      return 1 - 1.0 / (2 * n);
    }
    return p;
  }
}
