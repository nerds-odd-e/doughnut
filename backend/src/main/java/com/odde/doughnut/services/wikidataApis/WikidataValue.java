package com.odde.doughnut.services.wikidataApis;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.entities.Coordinate;
import com.odde.doughnut.services.wikidataApis.thirdPartyEntities.WikidataDatavalue;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public record WikidataValue(WikidataDatavalue datavalue) {

  public WikidataId toWikiClass() {
    return new WikidataId(datavalue.mustGetWikibaseEntityId());
  }

  public Coordinate getCoordinate() {
    JsonNode globeCoordinate = datavalue.tryGetGlobeCoordinate();
    if (globeCoordinate != null) {
      var latitude = globeCoordinate.get("latitude").asDouble();
      var longitude = globeCoordinate.get("longitude").asDouble();
      return new Coordinate(latitude, longitude);
    }
    return new Coordinate(datavalue.mustGetStringValue());
  }

  public String format() {
    datavalue.assertTimeType();
    DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("dd MMMM yyyy")
            .withZone(ZoneId.systemDefault())
            .localizedBy(Locale.ENGLISH);
    String inputTime = datavalue.getValue().get("time").textValue();
    Instant instant = Instant.parse(inputTime.substring(1));
    return formatter.format(instant) + (inputTime.startsWith("-") ? " B.C." : "");
  }
}
