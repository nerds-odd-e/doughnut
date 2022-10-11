package com.odde.doughnut.services.externalApis;

import com.odde.doughnut.entities.json.WikidataEntityData;
import java.util.Map;

public class WikidataEntityDataHash {
  public Map<String, WikidataInfo> entities;

  public WikidataEntityData getWikidataEntity(String wikidataId) {
    WikidataInfo wikidataInfo = entities.get(wikidataId);
    return new WikidataEntityData(
        wikidataInfo.GetEnglishTitle(), wikidataInfo.GetEnglishWikipediaUrl());
  }
}
