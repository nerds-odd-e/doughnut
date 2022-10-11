package com.odde.doughnut.services.externalApis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.odde.doughnut.entities.Coordinate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WikidataEntity {
  private String type;
  private String id;
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

  public String getHumanDescription(WikidataApi wikidataApi) {
    return Stream.of(
            getCountryOfOriginValue()
                .flatMap(wikidataId -> wikidataId.fetchEnglishTitleFromApi(wikidataApi)),
            getBirthdayData().map(WikidataDate::format))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(value -> !value.isBlank())
        .collect(Collectors.joining(", "));
  }

  public Optional<String> getCountryDescription() {
    return getFirstClaimValue("P625")
        .map(wikidataValue -> wikidataValue.toLocationDescription(wikidataValue.getCoordinate()));
  }

  public Optional<WikidataId> getInstanceOf() {
    return getFirstClaimValue("P31").map(WikidataValue::toWikiClass);
  }

  public Optional<Coordinate> getCoordinate() {
    return getFirstClaimValue("P625").flatMap(WikidataValue::getCoordinate);
  }

  private Optional<WikidataDate> getBirthdayData() {
    return getFirstClaimValue("P569").map(WikidataValue::toDateDescription);
  }

  private Optional<WikidataId> getCountryOfOriginValue() {
    return getFirstClaimValue("P27").map(WikidataValue::toWikiClass);
  }
}
