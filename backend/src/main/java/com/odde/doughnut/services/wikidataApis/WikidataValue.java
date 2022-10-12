package com.odde.doughnut.services.wikidataApis;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.entities.Coordinate;
import com.odde.doughnut.models.TimestampOperations;
import com.odde.doughnut.services.wikidataApis.thirdPartyEntities.WikidataDatavalue;

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
