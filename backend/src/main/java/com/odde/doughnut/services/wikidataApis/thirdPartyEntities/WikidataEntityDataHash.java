package com.odde.doughnut.services.wikidataApis.thirdPartyEntities;

import com.odde.doughnut.controllers.json.WikidataEntityData;
import java.util.Map;

public class WikidataEntityDataHash {
  public Map<String, WikidataEntityDataEntity> entities;

  public WikidataEntityData getWikidataEntity(String wikidataId) {
    WikidataEntityDataEntity wikidataInfo = entities.get(wikidataId);
    return new WikidataEntityData(
        wikidataInfo.GetEnglishTitle(), wikidataInfo.GetEnglishWikipediaUrl());
  }
}
