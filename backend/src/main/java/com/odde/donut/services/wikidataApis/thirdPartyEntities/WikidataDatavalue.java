package com.odde.donut.services.wikidataApis.thirdPartyEntities;

import lombok.Data;
import tools.jackson.databind.JsonNode;

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
    return value.stringValue(null);
  }

  public String mustGetWikibaseEntityId() {
    assertType("wikibase-entityid");
    return value.get("id").stringValue(null);
  }

  public JsonNode tryGetGlobeCoordinate() {
    if ("globecoordinate".compareToIgnoreCase(type) != 0) {
      return null;
    }
    return value;
  }

  public String mustGetISOTime() {
    assertType("time");
    return getValue().get("time").stringValue(null);
  }
}
