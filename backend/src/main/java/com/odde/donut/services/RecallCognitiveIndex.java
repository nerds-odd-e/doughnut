package com.odde.donut.services;

/**
 * The composite morning cognitive index's formula (slice 21.2, developer-approved — see "The index"
 * section of the plan): {@code 100 − 10 × mean(zA, zPace, zLapse, zConsistency)}, equal-weight, no
 * per-component tuning.
 *
 * <p>Each input must already be a signed z-score where positive means worse-than-usual. {@code zA}
 * (accuracy) and {@code zLapse} are sign-flipped from their raw values before reaching here —
 * deciding how and where that flip happens is out of scope for this pure formula and belongs to the
 * caller that has real per-morning values to wire up (slice 21.3).
 */
final class RecallCognitiveIndex {
  private RecallCognitiveIndex() {}

  static double compute(double zA, double zPace, double zLapse, double zConsistency) {
    double mean = (zA + zPace + zLapse + zConsistency) / 4.0;
    return 100 - 10 * mean;
  }
}
