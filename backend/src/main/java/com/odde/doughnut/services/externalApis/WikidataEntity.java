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

  private Optional<String> getHumanDescription(WikidataApi wikidataApi) {
    String description =
        Stream.of(
                getCountryOfOriginValue()
                    .map(WikidataValue::toWikiClass)
                    .flatMap(
                        wikidataId ->
                            wikidataApi
                                .getWikidataEntityData(wikidataId)
                                .map(e -> e.WikidataTitleInEnglish))
                    .orElse(""),
                getBirthdayData().map(WikidataValue::toDateDescription).orElse(""))
            .filter(value -> !value.isBlank())
            .collect(Collectors.joining(", "));

    return Optional.of(description);
  }

  private Optional<String> getCountryDescription() {
    return getFirstClaimValue("P625").map(WikidataValue::toLocationDescription);
  }

  public Optional<String> getDescription(WikidataApi wikidataApi) {
    if (getInstanceOfx().orElse(false)) {
      return getHumanDescription(wikidataApi);
    }
    return getCountryDescription();
  }

  private Optional<Boolean> getInstanceOfx() {
    return getInstanceOfy().map(x -> x.equals("Q5"));
  }

  private Optional<String> getInstanceOfy() {
    return getInstanceOf().map(WikidataValue::toWikiClass);
  }

  public Optional<Coordinate> getCoordinate() {
    return getFirstClaimValue("P625").flatMap(WikidataValue::getCoordinate);
  }

  private Optional<WikidataValue> getBirthdayData() {
    return getFirstClaimValue("P569");
  }

  private Optional<WikidataValue> getCountryOfOriginValue() {
    return getFirstClaimValue("P27");
  }

  private Optional<WikidataValue> getInstanceOf() {
    return getFirstClaimValue("P31");
  }
}
