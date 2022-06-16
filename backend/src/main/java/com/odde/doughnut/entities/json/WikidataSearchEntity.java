package com.odde.doughnut.entities.json;

import java.util.Map;

public class WikidataSearchEntity {
  public String label;
  public String description;

  public WikidataSearchEntity(Map<String, Object> object) {
    this.label = correctionString(object.get("label")).toString();
    this.description = correctionString(object.get("description")).toString();
  }

  private Object correctionString(Object object) {
    return (object != null) ? object : "";
  }
}
