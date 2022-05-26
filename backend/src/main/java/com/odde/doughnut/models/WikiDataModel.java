package com.odde.doughnut.models;

import java.util.Map;

public class WikiDataModel {
  public Map<String, WikiDataInfo> entities;

  public WikiDataInfo GetInfoForWikiDataId(String wikiDataId) {
    if (entities.containsKey(wikiDataId)) {
      return entities.get(wikiDataId);
    }
    return new WikiDataInfo();
  }
}
