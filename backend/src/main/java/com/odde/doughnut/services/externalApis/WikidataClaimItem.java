package com.odde.doughnut.services.externalApis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Optional;
import lombok.Data;

@Data
public class WikidataClaimItem {
  static String DATAVALUE_KEY = "datavalue";
  static String VALUE_KEY = "value";
  static String VALUE_TYPE_KEY = "type";

  private String type;
  private String id;
  private Map<String, JsonNode> mainsnak;

  @JsonIgnore
  public Optional<WikidataValue> getValue() {
    if (!mainsnak.containsKey(DATAVALUE_KEY)) {
      return Optional.empty();
    }
    JsonNode dataValue = mainsnak.get(DATAVALUE_KEY);
    return Optional.of(
        new WikidataValue(dataValue.get(VALUE_KEY), dataValue.get(VALUE_TYPE_KEY).textValue()));
  }
}
