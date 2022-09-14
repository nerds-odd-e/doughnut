package com.odde.doughnut.services.externalApis;

import java.util.Map;

public class WikidataValue {
  private Map<String, Object> data = null;
  private String stringValue = null;

  public WikidataValue(Map<String, Object> data) {
    this.data = data;
  }

  public WikidataValue(String stringValue) {
    this.stringValue = stringValue;
  }

  String toLocationDescription() {
    if (data == null) {
      return "Location: " + stringValue;
    }
    return "Location: "
        + data.get("latitude").toString()
        + "'N, "
        + data.get("longitude").toString()
        + "'E";
  }
}
