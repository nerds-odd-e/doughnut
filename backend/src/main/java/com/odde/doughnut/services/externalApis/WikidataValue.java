package com.odde.doughnut.services.externalApis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class WikidataValue {
  private Map<String, Object> data = null;
  private String stringValue = null;
  private String type;

  public WikidataValue(JsonNode value, String type) {
    this.type = type;
    if (isGlobeCoordinate(type)) {
      this.data =
          new ObjectMapper().convertValue(value, new TypeReference<Map<String, Object>>() {});
      return;
    }
    if (isStringValue(type)) {
      stringValue = value.textValue();
      return;
    }
    throw new RuntimeException("Unsupported wikidata value type: " + type);
  }

  private boolean isStringValue(String type) {
    return "string".compareToIgnoreCase(type) == 0;
  }

  private boolean isGlobeCoordinate(String type) {
    return "globecoordinate".compareToIgnoreCase(type) == 0;
  }

  String toLocationDescription() {
    if (isGlobeCoordinate(type)) {
      return "Location: "
          + data.get("latitude").toString()
          + "'N, "
          + data.get("longitude").toString()
          + "'E";
    }
    return "Location: " + stringValue;
  }

  String toHumanDescription() {
    return "31 March 1980, Taiwan";
  }
}
