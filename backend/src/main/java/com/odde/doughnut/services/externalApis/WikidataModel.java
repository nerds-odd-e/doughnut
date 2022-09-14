package com.odde.doughnut.services.externalApis;

import com.odde.doughnut.entities.json.WikidataEntity;
import java.util.Map;

public class WikidataModel {
  public Map<String, WikidataInfo> entities;

  public WikidataEntity getWikidataEntity(String wikidataId) {
    WikidataInfo wikidataInfo = entities.get(wikidataId);
    return new WikidataEntity(
        wikidataInfo.GetEnglishTitle(), wikidataInfo.GetEnglishWikipediaUrl());
  }
}
