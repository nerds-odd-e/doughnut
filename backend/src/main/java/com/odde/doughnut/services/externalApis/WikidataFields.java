package com.odde.doughnut.services.externalApis;

public enum WikidataFields {

  BIRTHDAY("P569"),
  COORDINATE_LOCATION("P625"),
  INSTANCE_OF("P31"),
  COUNTRY_OF_CITIZENSHIP("P27");

  public final String label;

  WikidataFields(String label){
    this.label = label;
  }

}
