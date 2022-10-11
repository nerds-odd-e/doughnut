package com.odde.doughnut.services.externalApis;

import java.util.Optional;

public record WikidataId(String wikidataId) {
  public boolean isHuman() {
    return "Q5".equals(wikidataId);
  }

  Optional<String> fetchEnglishTitleFromApi(WikidataApi wikidataApi) {
    return wikidataApi.getWikidataEntityData(wikidataId()).map(e -> e.WikidataTitleInEnglish);
  }
}
