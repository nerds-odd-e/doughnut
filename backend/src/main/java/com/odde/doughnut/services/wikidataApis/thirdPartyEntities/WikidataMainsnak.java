package com.odde.doughnut.services.wikidataApis.thirdPartyEntities;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import lombok.Data;

@Data
public class WikidataMainsnak {
  private Map<String, JsonNode> datavalue;

  public String getType() {
    return getDatavalue().get(WikidataClaimItem.VALUE_TYPE_KEY).textValue();
  }

  public JsonNode getValue() {
    return getDatavalue().get(WikidataClaimItem.VALUE_KEY);
  }

  public void assertTimeType() {
    if (!("time".compareToIgnoreCase(getType()) == 0)) {
      throw new RuntimeException(
          "Unsupported wikidata value type: " + getType() + ", expected time");
    }
  }

  public void assertWikibaseType() {
    if (!("wikibase-entityid".compareToIgnoreCase(getType()) == 0)) {
      throw new RuntimeException(
          "Unsupported wikidata value type: " + getType() + ", expected wikibase-entityid");
    }
  }

  public boolean isGlobeCoordinate() {
    return "globecoordinate".compareToIgnoreCase(getType()) == 0;
  }

  public void assertStringType() {
    if ("string".compareToIgnoreCase(getType()) != 0) {
      throw new RuntimeException(
          "Unsupported wikidata value type: " + getType() + ", expected string");
    }
  }
}
