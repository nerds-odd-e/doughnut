package com.odde.doughnut.services.wikidataApis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Coordinate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

public record WikidataValue(
    com.odde.doughnut.services.wikidataApis.thirdPartyEntities.WikidataMainsnak mainsnak) {

  public WikidataId toWikiClass() {
    mainsnak.assertWikibaseType();
    return new WikidataId(this.mainsnak.getValue().get("id").textValue());
  }

  public Coordinate getCoordinate() {
    if (mainsnak.isGlobeCoordinate()) {
      Map<String, Object> data =
          new ObjectMapper().convertValue(this.mainsnak.getValue(), new TypeReference<>() {});

      var latitude = (Double) data.get("latitude");
      var longitude = (Double) data.get("longitude");
      return new Coordinate(latitude, longitude);
    }
    return new Coordinate(getStringValue());
  }

  private String getStringValue() {
    mainsnak.assertStringType();
    return mainsnak.getValue().textValue();
  }

  public String format() {
    mainsnak.assertTimeType();
    DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("dd MMMM yyyy")
            .withZone(ZoneId.systemDefault())
            .localizedBy(Locale.ENGLISH);
    String inputTime = mainsnak.getValue().get("time").textValue();
    Instant instant = Instant.parse(inputTime.substring(1));
    return formatter.format(instant) + (inputTime.startsWith("-") ? " B.C." : "");
  }
}
