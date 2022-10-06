package com.odde.doughnut.services.externalApis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

public class WikidataValue {
  private Map<String, Object> data = null;
  private String stringValue = null;
  private String type;
  private String wikiClass;

  private String timeValue;

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
    if (isTimeValue(type)) {
      DateTimeFormatter formatter =
          DateTimeFormatter.ofPattern("dd MMMM yyyy")
              .withZone(ZoneId.systemDefault())
              .localizedBy(Locale.ENGLISH);
      String inputTime = value.get("time").textValue();
      Instant instant = Instant.parse(inputTime.substring(1));
      timeValue = formatter.format(instant);
      String postFix = inputTime.startsWith("-") ? " B.C." : "";
      timeValue += postFix;

      return;
    }
    if (isWikibase(type)) {
      wikiClass = value.get("id").textValue();
      return;
    }

    throw new RuntimeException("Unsupported wikidata value type: " + type);
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

  public String toDateDescription() {
    return timeValue;
  }

  public String toWikiClass() {
    return wikiClass;
  }

  String getStringValue() {
    return stringValue;
  }
}
