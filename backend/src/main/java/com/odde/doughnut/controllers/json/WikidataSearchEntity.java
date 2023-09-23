package com.odde.doughnut.controllers.json;

import java.util.Map;

public class WikidataSearchEntity {
  public String id;
  public String label;
  public String description;

  public WikidataSearchEntity(Map<String, Object> object) {
    this.id = correction(object.get("id")).toString();
    this.label = correction(object.get("label")).toString();
    this.description = correction(object.get("description")).toString();
  }

  private Object correction(Object object) {
    return (object != null) ? object : "";
  }
}
