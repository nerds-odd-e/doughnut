package com.odde.doughnut.services.wikidataApis.thirdPartyEntities;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class WikidataDatavalue {
  private String type;
  private JsonNode value;

  private void assertType(String expectedType) {
    if (expectedType.compareToIgnoreCase(type) != 0) {
      throw new RuntimeException(
          "Unexpected wikidata value type: " + type + ", expected " + expectedType);
    }
  }

  public String mustGetStringValue() {
    assertType("string");
    return value.textValue();
  }

  public String mustGetWikibaseEntityId() {
    assertType("wikibase-entityid");
    return value.get("id").textValue();
  }

  public JsonNode tryGetGlobeCoordinate() {
    if ("globecoordinate".compareToIgnoreCase(type) != 0) {
      return null;
    }
    return value;
  }

  public String mustGetISOTime() {
    assertType("time");
    return getValue().get("time").textValue();
  }
}
