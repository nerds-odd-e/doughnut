package com.odde.doughnut.services.wikidataApis.thirdPartyEntities;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class WikidataDatavalue {
  private String type;
  private JsonNode value;

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
