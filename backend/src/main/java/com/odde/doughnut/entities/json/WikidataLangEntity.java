package com.odde.doughnut.entities.json;

import java.util.Map;

public class WikidataLangEntity {
  public String value;
  public String language;

  public WikidataLangEntity(
      Map<String, Object> obj) {
    this.value = obj.get("value").toString();
    this.language = obj.get("language").toString();
  }
}
