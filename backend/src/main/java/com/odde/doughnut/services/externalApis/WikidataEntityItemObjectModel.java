package com.odde.doughnut.services.externalApis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.Data;

@Data
public class WikidataEntityItemObjectModel {
  static String DATAVALUE_KEY = "datavalue";
  static String VALUE_KEY = "value";
  static String VALUE_TYPE_KEY = "type";

  static class VALUE_TYPE {
    public static String GLOBE_COORDINATE = "globecoordinate";
    public static String STRING = "string";
  }

  private String type;
  private String id;
  private Map<String, JsonNode> mainsnak;

  @JsonIgnore
  public WikidataValue getValue() {
    if (!mainsnak.containsKey(DATAVALUE_KEY)) {
      return null;
    }
    ObjectMapper mapper = new ObjectMapper();
    JsonNode value = mainsnak.get(DATAVALUE_KEY);
    if (VALUE_TYPE.GLOBE_COORDINATE.compareToIgnoreCase(value.get(VALUE_TYPE_KEY).textValue())
        == 0) {
      return new WikidataValue(
          mapper.convertValue(value.get(VALUE_KEY), new TypeReference<Map<String, Object>>() {}));
    } else if (VALUE_TYPE.STRING.compareToIgnoreCase(value.get(VALUE_TYPE_KEY).textValue()) == 0) {
      String stringValue =
          mapper.convertValue(value.get(VALUE_KEY), new TypeReference<String>() {});
      return new WikidataValue(stringValue);
    }
    return null;
  }
}
