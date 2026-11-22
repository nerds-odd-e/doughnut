package com.odde.donut.services.ai;

public class RefinementLayoutUnavailable extends RuntimeException {
  public static final String OUTPUT_LIMIT = "Generation could not finish within its limit.";
  public static final String UNUSABLE = "AI could not generate a refinement layout.";

  private RefinementLayoutUnavailable(String explanation) {
    super(explanation);
  }

  public static RefinementLayoutUnavailable outputLimit() {
    return new RefinementLayoutUnavailable(OUTPUT_LIMIT);
  }

  public static RefinementLayoutUnavailable unusable() {
    return new RefinementLayoutUnavailable(UNUSABLE);
  }
}
