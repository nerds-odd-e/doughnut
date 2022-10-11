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
                getFirstClaimValue("P27")
                    .map(WikidataValue::toWikiClass)
                    .flatMap(
                        wikidataId ->
                            wikidataApi
                                .getWikidataEntityData(wikidataId)
                                .map(e -> e.WikidataTitleInEnglish))
                    .orElse(""),
                getFirstClaimValue("P569").map(WikidataValue::toDateDescription).orElse(""))
            .filter(value -> !value.isBlank())
            .collect(Collectors.joining(", "));

    return Optional.of(description);
  }

  private Optional<String> getCountryDescription() {
    return getFirstLocationClaimValue().map(WikidataValue::toLocationDescription);
  }

  private boolean isInstanceOf(WikidataItems human) {
    return getFirstClaimValue("P31")
        .map(WikidataValue::toWikiClass)
        .equals(Optional.of(human.label));
  }

  public Optional<String> getDescription(WikidataApi wikidataApi) {
    if (isInstanceOf(WikidataItems.HUMAN)) {
      return getHumanDescription(wikidataApi);
    }
    return getCountryDescription();
  }

  public Optional<Coordinate> getCoordinate() {
    return getFirstLocationClaimValue().flatMap(WikidataValue::getCoordinate);
  }

  private Optional<WikidataValue> getFirstLocationClaimValue() {
    return getFirstClaimValue("P625");
  }
}
