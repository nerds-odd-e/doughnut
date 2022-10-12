package com.odde.doughnut.services.wikidataApis;

public record WikidataId(String wikidataId) {
  public boolean isHuman() {
    return "Q5".equals(wikidataId);
  }

  public WikidataIdWithApi withApi(WikidataApi wikidataApi) {
    return new WikidataIdWithApi(wikidataId, wikidataApi);
  }
}
