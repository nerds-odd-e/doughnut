package com.odde.doughnut.entities.json;

import java.util.Map;

public class WikidataMatchEntity {
  public String type;
  public String language;
  public String text;

  public WikidataMatchEntity(Map<String, Object> obj) {
    this.type = obj.get("type").toString();
    this.language = obj.get("language").toString();
    this.text = obj.get("text").toString();
  }
}
