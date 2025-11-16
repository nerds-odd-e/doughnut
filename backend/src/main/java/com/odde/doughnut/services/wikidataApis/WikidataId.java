package com.odde.doughnut.services.wikidataApis;

import com.odde.doughnut.services.TimestampService;

public record WikidataId(String wikidataId) {
  public boolean isHuman() {
    return "Q5".equals(wikidataId);
  }

  public WikidataIdWithApi withApi(WikidataApi wikidataApi, TimestampService timestampService) {
    return WikidataIdWithApi.create(wikidataId, wikidataApi, timestampService);
  }
}
