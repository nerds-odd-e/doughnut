package com.odde.donut.services;

/**
 * The composite morning cognitive index's formula (slice 21.2, developer-approved — see "The index"
 * section of the plan): {@code 100 − 10 × mean(zA, zPace, zLapse, zConsistency)}, equal-weight, no
 * per-component tuning.
 *
 * <p>Each input must already be a signed z-score where positive means worse-than-usual. Of the
 * four, only {@code zA} (accuracy) actually needs sign-flipping from its raw value: raw accuracy is
 * higher-is-better, so it is negated before reaching here. {@code zPace}, {@code zLapse}, and
 * {@code zConsistency} are already higher-is-worse in their raw form (more lapses, more time drift,
 * more erratic spread), so their day-baseline z-scores need no flip. Deciding how and where any
 * flip happens is out of scope for this pure formula and belongs to the caller that has real
 * per-morning values to wire up (slice 21.3, see {@link RecallMorningHalfIndex#compute}).
 */
final class RecallCognitiveIndex {
  private RecallCognitiveIndex() {}

  static double compute(double zA, double zPace, double zLapse, double zConsistency) {
    double mean = (zA + zPace + zLapse + zConsistency) / 4.0;
    return 100 - 10 * mean;
  }
}
