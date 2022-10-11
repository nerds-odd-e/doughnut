package com.odde.doughnut.services.externalApis;

public record WikidataId(String wikidataId) {
  public boolean isHuman() {
    return "Q5".equals(wikidataId);
  }
}
