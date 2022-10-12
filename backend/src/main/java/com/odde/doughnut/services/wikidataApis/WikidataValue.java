package com.odde.doughnut.services.wikidataApis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Coordinate;
import com.odde.doughnut.services.wikidataApis.thirdPartyEntities.WikidataClaimItem;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

public class WikidataValue {
  private Map<String, Object> data = null;
  private String stringValue = null;
  private WikidataClaimItem wikidataClaimItem;
  private String type;
  private String wikiClass;

  public WikidataValue(WikidataClaimItem wikidataClaimItem, JsonNode value, String type) {
    this.wikidataClaimItem = wikidataClaimItem;
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

    if (isWikibase(type)) {
      wikiClass = value.get("id").textValue();
      return;
    }
  }

  private boolean isWikibase(String type) {
    return "wikibase-entityid".compareToIgnoreCase(type) == 0;
  }

  private boolean isTimeValue(String type) {
    return "time".compareToIgnoreCase(type) == 0;
  }

  private boolean isStringValue(String type) {
    return "string".compareToIgnoreCase(type) == 0;
  }

  private boolean isGlobeCoordinate(String type) {
    return "globecoordinate".compareToIgnoreCase(type) == 0;
  }

  public WikidataId toWikiClass() {
    return new WikidataId(wikiClass);
  }

  public Coordinate getCoordinate() {
    if (isGlobeCoordinate(type)) {
      var latitude = (Double) data.get("latitude");
      var longitude = (Double) data.get("longitude");
      return new Coordinate(latitude, longitude);
    }

    return new Coordinate(stringValue);
  }

  public String format() {
    if (!isTimeValue(type)) {
      throw new RuntimeException("Unsupported wikidata value type: " + type + ", expected time");
    }
    DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("dd MMMM yyyy")
            .withZone(ZoneId.systemDefault())
            .localizedBy(Locale.ENGLISH);
    String inputTime = this.wikidataClaimItem.getValue1().get("time").textValue();
    Instant instant = Instant.parse(inputTime.substring(1));
    return formatter.format(instant) + (inputTime.startsWith("-") ? " B.C." : "");
  }
}
