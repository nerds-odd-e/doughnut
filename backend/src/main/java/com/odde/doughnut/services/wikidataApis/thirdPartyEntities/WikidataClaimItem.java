package com.odde.doughnut.services.wikidataApis.thirdPartyEntities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.services.wikidataApis.WikidataValue;
import java.util.Map;
import java.util.Optional;
import lombok.Data;

@Data
public class WikidataClaimItem {
  static String DATA_VALUE_KEY = "datavalue";
  static String VALUE_KEY = "value";
  static String VALUE_TYPE_KEY = "type";

  private Map<String, JsonNode> mainsnak;

  @JsonIgnore
  public Optional<WikidataValue> getValue() {
    if (!mainsnak.containsKey(DATA_VALUE_KEY)) {
      return Optional.empty();
    }
    return Optional.of(
        new WikidataValue(this, getValue1(), getDataValue().get(VALUE_TYPE_KEY).textValue()));
  }

  public JsonNode getValue1() {
    return getDataValue().get(VALUE_KEY);
  }

  private JsonNode getDataValue() {
    return mainsnak.get(DATA_VALUE_KEY);
  }
}
