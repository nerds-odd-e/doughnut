package com.odde.doughnut.services.externalApis;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
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
  Map<String, Object> data;

  @JsonProperty("mainsnak")
  private void unpackNested(Map<String, JsonNode> mainsnak) {
    if (mainsnak.containsKey(DATAVALUE_KEY) && mainsnak.get(DATAVALUE_KEY).has(VALUE_KEY)) {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode value = mainsnak.get(DATAVALUE_KEY);
      if (VALUE_TYPE.GLOBE_COORDINATE.compareToIgnoreCase(value.get(VALUE_TYPE_KEY).textValue())
          == 0) {
        data =
            mapper.convertValue(
                mainsnak.get(DATAVALUE_KEY).get(VALUE_KEY),
                new TypeReference<Map<String, Object>>() {});
      } else if (VALUE_TYPE.STRING.compareToIgnoreCase(value.get(VALUE_TYPE_KEY).textValue())
          == 0) {
        String stringValue =
            mapper.convertValue(
                mainsnak.get(DATAVALUE_KEY).get(VALUE_KEY), new TypeReference<String>() {});
        data = new LinkedHashMap<>();
        data.put(VALUE_KEY, stringValue);
      }
    }
  }
}
