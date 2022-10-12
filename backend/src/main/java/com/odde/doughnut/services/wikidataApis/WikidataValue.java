package com.odde.doughnut.services.wikidataApis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Coordinate;
import com.odde.doughnut.services.wikidataApis.thirdPartyEntities.WikidataDatavalue;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

public record WikidataValue(WikidataDatavalue datavalue) {

  public WikidataId toWikiClass() {
    datavalue.assertWikibaseType();
    return new WikidataId(datavalue.getValue().get("id").textValue());
  }

  public Coordinate getCoordinate() {
    if (datavalue.isGlobeCoordinate()) {
      Map<String, Object> data =
          new ObjectMapper().convertValue(datavalue.getValue(), new TypeReference<>() {});

      var latitude = (Double) data.get("latitude");
      var longitude = (Double) data.get("longitude");
      return new Coordinate(latitude, longitude);
    }
    return new Coordinate(getStringValue());
  }

  private String getStringValue() {
    datavalue.assertStringType();
    return datavalue.getValue().textValue();
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
