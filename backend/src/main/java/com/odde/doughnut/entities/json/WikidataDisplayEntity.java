package com.odde.doughnut.entities.json;

import java.util.Map;

public class WikidataDisplayEntity {
  public WikidataLangEntity label;
  public WikidataLangEntity description;

  public WikidataDisplayEntity(Map<String, Object> obj) {
    this.label = new WikidataLangEntity((Map<String, Object>) obj.get("label"));
    this.description = new WikidataLangEntity((Map<String, Object>) obj.get("description"));
  }
}
