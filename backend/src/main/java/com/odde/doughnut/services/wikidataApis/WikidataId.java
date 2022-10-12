package com.odde.doughnut.services.wikidataApis;

import java.util.Optional;

public record WikidataId(String wikidataId) {
  public boolean isHuman() {
    return "Q5".equals(wikidataId);
  }

  public Optional<String> fetchEnglishTitleFromApi(WikidataApi wikidataApi) {
    return wikidataApi.getWikidataEntityData(wikidataId()).map(e -> e.WikidataTitleInEnglish);
  }
}
