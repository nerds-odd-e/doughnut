package com.odde.doughnut.services.externalApis;

public enum WikidataItems {
  HUMAN("Q5"),
  BOOK("Q571");

  public final String label;

  WikidataItems(String label) {
    this.label = label;
  }
}
