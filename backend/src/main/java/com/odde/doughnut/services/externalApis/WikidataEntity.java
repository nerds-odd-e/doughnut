package com.odde.doughnut.services.externalApis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.odde.doughnut.entities.Coordinate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WikidataEntity {
  Map<String, List<WikidataClaimItem>> claims;

  private Optional<WikidataValue> getFirstClaimValue(String propertyId) {
    if (claims == null) {
      return Optional.empty();
    }
    List<WikidataClaimItem> listOfItems = claims.getOrDefault(propertyId, null);
    if (listOfItems == null || listOfItems.isEmpty()) {
      return Optional.empty();
    }
    return listOfItems.get(0).getValue();
  }

  public Optional<Coordinate> getGeographicCoordinate() {
    return getFirstClaimValue("P625").map(WikidataValue::getCoordinate);
  }

  public Optional<WikidataId> getInstanceOf() {
    return getFirstClaimValue("P31").map(WikidataValue::toWikiClass);
  }

  public Optional<Coordinate> getCoordinate() {
    return getFirstClaimValue("P625").map(WikidataValue::getCoordinate);
  }

  public Optional<WikidataDate> getBirthdayData() {
    return getFirstClaimValue("P569").map(WikidataValue::toDateDescription);
  }

  public Optional<WikidataId> getCountryOfOriginValue() {
    return getFirstClaimValue("P27").map(WikidataValue::toWikiClass);
  }
}
