package com.odde.doughnut.services.wikidataApis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Coordinate;
import com.odde.doughnut.services.wikidataApis.thirdPartyEntities.WikidataClaimItem;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

public record WikidataValue(WikidataClaimItem wikidataClaimItem) {

  public WikidataId toWikiClass() {
    wikidataClaimItem.assertWikibaseType();
    return new WikidataId(this.wikidataClaimItem.getValue1().get("id").textValue());
  }

  public Coordinate getCoordinate() {
    if (wikidataClaimItem.isGlobeCoordinate()) {
      Map<String, Object> data =
          new ObjectMapper()
              .convertValue(this.wikidataClaimItem.getValue1(), new TypeReference<>() {});

      var latitude = (Double) data.get("latitude");
      var longitude = (Double) data.get("longitude");
      return new Coordinate(latitude, longitude);
    }
    return new Coordinate(getStringValue());
  }

  private String getStringValue() {
    wikidataClaimItem.assertStringType();
    return wikidataClaimItem.getValue1().textValue();
  }

  public String format() {
    wikidataClaimItem.assertTimeType();
    DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("dd MMMM yyyy")
            .withZone(ZoneId.systemDefault())
            .localizedBy(Locale.ENGLISH);
    String inputTime = wikidataClaimItem.getValue1().get("time").textValue();
    Instant instant = Instant.parse(inputTime.substring(1));
    return formatter.format(instant) + (inputTime.startsWith("-") ? " B.C." : "");
  }
}
