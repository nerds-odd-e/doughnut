package com.odde.doughnut.entities.json;

import java.util.HashMap;
import java.util.Map;

public class WikidataDisplayEntity {
  public WikidataLangEntity label;
  public WikidataLangEntity description;

  public WikidataDisplayEntity(Map<String, Object> obj) {
    Map<String, Object> emptys = new HashMap<>();
    emptys.put("value", "");
    emptys.put("language", "");
    this.label =
        new WikidataLangEntity(
            (obj.get("label") != null) ? (Map<String, Object>) obj.get("label") : emptys);
    this.description =
        new WikidataLangEntity(
            (obj.get("description") != null)
                ? (Map<String, Object>) obj.get("description")
                : emptys);
  }
}
