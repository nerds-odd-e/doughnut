package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

/**
 * Covers plan {@code 008-probe-convergent-analyses} slice 2: the EZ-diffusion closed-form recovery
 * equations (Wagenmakers, van der Maas &amp; Grasman, 2007). Round-trip cases forward-compute
 * {@code (P, MRT, VRT)} from a target {@code (v, a, Ter)} using the paper's own forward equations
 * (Equation 4 for Pc, the paper's Appendix {@code get.vaTer} definition of mean decision time for
 * Ter, and the Palmer/Huk/Shadlen 2005 hyperbolic form the paper cites as equivalent to its
 * Equation 6 for VRT — cross-checked against the paper's own worked numerical example: v=0.1,
 * a=0.14, Ter=0.300 forward-predicts MRT=0.723, VRT=0.112, Pc=.802, reproduced by {@link
 * #forwardMrt}/{@link #forwardVrt}/{@link #forwardPc} below), then asserts {@link
 * EzDiffusion#recover} reconstructs the original target within tolerance.
 */
class EzDiffusionTest {
  private static final double S = 0.1;
  private static final double S2 = S * S;

  @Test
  void recoversDriftBoundaryAndNondecisionTimeFromForwardPredictedMoments() {
    // The paper's own Appendix worked example.
    double v = 0.1;
    double a = 0.14;
    double ter = 0.300;
    int n = 100_000; // large n: Pc is far from 0/1/0.5, so no edge correction is exercised here.

    double pc = forwardPc(v, a);
    double mrt = forwardMrt(v, a, ter);
    double vrt = forwardVrt(v, a);

    EzDiffusion.Parameters recovered = EzDiffusion.recover(pc, mrt, vrt, n);

    assertThat(recovered.driftRate(), closeTo(v, 0.0001));
    assertThat(recovered.boundarySeparation(), closeTo(a, 0.0001));
    assertThat(recovered.nondecisionTime(), closeTo(ter, 0.0001));
  }

  @Test
  void recoversNegativeDriftBelowChanceAccuracy() {
    double v = -0.2;
    double a = 0.11;
    double ter = 0.25;
    int n = 100_000;

    double pc = forwardPc(v, a);
    double mrt = forwardMrt(v, a, ter);
    double vrt = forwardVrt(v, a);

    EzDiffusion.Parameters recovered = EzDiffusion.recover(pc, mrt, vrt, n);

    assertThat(recovered.driftRate(), closeTo(v, 0.0001));
    assertThat(recovered.boundarySeparation(), closeTo(a, 0.0001));
    assertThat(recovered.nondecisionTime(), closeTo(ter, 0.0001));
  }

  @Test
  void zeroCorrectAppliesTheOneHalfErrorEdgeCorrectionBeforeRecovering() {
    int n = 50;
    double mrt = 0.6;
    double vrt = 0.05;

    EzDiffusion.Parameters atZero = EzDiffusion.recover(0.0, mrt, vrt, n);
    EzDiffusion.Parameters atEdgeCorrectedValue = EzDiffusion.recover(1.0 / (2 * n), mrt, vrt, n);

    assertThat(atZero.driftRate(), equalTo(atEdgeCorrectedValue.driftRate()));
    assertThat(atZero.boundarySeparation(), equalTo(atEdgeCorrectedValue.boundarySeparation()));
    assertThat(atZero.nondecisionTime(), equalTo(atEdgeCorrectedValue.nondecisionTime()));
  }

  @Test
  void allCorrectAppliesTheOneHalfErrorEdgeCorrectionBeforeRecovering() {
    int n = 50;
    double mrt = 0.6;
    double vrt = 0.05;

    EzDiffusion.Parameters atOne = EzDiffusion.recover(1.0, mrt, vrt, n);
    EzDiffusion.Parameters atEdgeCorrectedValue =
        EzDiffusion.recover(1 - 1.0 / (2 * n), mrt, vrt, n);

    assertThat(atOne.driftRate(), equalTo(atEdgeCorrectedValue.driftRate()));
    assertThat(atOne.boundarySeparation(), equalTo(atEdgeCorrectedValue.boundarySeparation()));
    assertThat(atOne.nondecisionTime(), equalTo(atEdgeCorrectedValue.nondecisionTime()));
  }

  @Test
  void chanceAccuracyUsesTheLEqualsZeroLimitNotDivisionByLogitOfOneHalf() {
    // Pick a target boundary separation, forward-predict VRT via the paper's own stated v=0
    // special case of the forward variance equation (VRT = a^4 / (24 s^4)), then check recovery
    // reconstructs that same boundary separation with zero drift.
    double targetA = 0.12;
    double vrt = Math.pow(targetA, 4) / (24 * S2 * S2);
    double ter = 0.3;
    // Standard driftless-Wiener first-passage-time result for a start point at a/2, equidistant
    // from both boundaries: MDT = a^2 / (4s^2) (independent of, and not derived from, production
    // code — the classic z(a-z)/variance-rate expected-hitting-time formula with z = a/2).
    double mdt = (targetA * targetA) / (4 * S2);
    double mrt = mdt + ter;
    int n = 1_000;

    EzDiffusion.Parameters atChance = EzDiffusion.recover(0.5, mrt, vrt, n);

    assertThat(atChance.driftRate(), equalTo(0.0));
    assertThat(atChance.boundarySeparation(), closeTo(targetA, 0.000001));
    assertThat(atChance.nondecisionTime(), closeTo(ter, 0.000001));
  }

  @Test
  void fewerThanTwoTrialsYieldsNullParameters() {
    EzDiffusion.Parameters result = EzDiffusion.recover(0.8, 0.7, 0.1, 1);

    assertThat(result.driftRate(), nullValue());
    assertThat(result.boundarySeparation(), nullValue());
    assertThat(result.nondecisionTime(), nullValue());
  }

  @Test
  void nonPositiveVarianceYieldsNullParameters() {
    EzDiffusion.Parameters result = EzDiffusion.recover(0.8, 0.7, 0.0, 100);

    assertThat(result.driftRate(), nullValue());
    assertThat(result.boundarySeparation(), nullValue());
    assertThat(result.nondecisionTime(), nullValue());
  }

  /** Equation 4: Pc = 1 / (1 + exp(-av/s^2)). */
  private static double forwardPc(double v, double a) {
    return 1 / (1 + Math.exp(-v * a / S2));
  }

  /**
   * Equation 8 (MRT = MDT + Ter) with mean decision time from the paper's Appendix {@code
   * get.vaTer} definition (y = -va/s^2; MDT = (a/(2v)) * (1-exp(y))/(1+exp(y))).
   */
  private static double forwardMrt(double v, double a, double ter) {
    double y = -v * a / S2;
    double mdt = (a / (2 * v)) * (1 - Math.exp(y)) / (1 + Math.exp(y));
    return mdt + ter;
  }

  /**
   * Equation 6, in the hyperbolic form the paper cites (Palmer, Huk &amp; Shadlen, 2005, typo
   * corrected) as equivalent: VRT = z*{tanh(z*v*) - z*v*sech^2(z*v*)} / v*^3, where v*=v/s,
   * z*=(a/2)/s. Cross-checked against the paper's own stated v=0 special case (VRT = a^4/(24s^4))
   * as the y -> 0 limit, and against the paper's worked numerical example.
   */
  private static double forwardVrt(double v, double a) {
    double y = v * a / S2;
    double half = y / 2;
    double tanhHalf = Math.tanh(half);
    double sechHalf = 1 / Math.cosh(half);
    double bracket = tanhHalf - half * sechHalf * sechHalf;
    return (a * S2) / (2 * v * v * v) * bracket;
  }
}
