package com.odde.doughnut.services.externalApis;

public enum WikidataFields {
  BIRTHDAY("P569"),
  COORDINATE_LOCATION("P625"),
  INSTANCE_OF("P31"),
  COUNTRY_OF_CITIZENSHIP("P27"),
  AUTHOR("P50"),
  ISBN("P212"),
  TITLE("P1476");

  public final String label;

  WikidataFields(String label) {
    this.label = label;
  }
}
