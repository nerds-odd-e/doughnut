package com.odde.donut.services.wikidataApis;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.donut.entities.Coordinate;
import com.odde.donut.services.wikidataApis.thirdPartyEntities.WikidataDatavalue;
import com.odde.donut.utils.TimestampOperations;

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

  public String formattedTime() {
    return TimestampOperations.formatISOTimeToYearSupportingBC(datavalue.mustGetISOTime());
  }
}
