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
  Map<String, List<WikidataEntityItemObjectModel>> claims;

  private List<WikidataEntityItemObjectModel> getProperty(String propertyId) {
    if (claims == null) {
      return null;
    }
    return claims.getOrDefault(propertyId, null);
  }

  private Optional<WikidataValue> getFirstClaimOfProperty(String propertyId) {
    List<WikidataEntityItemObjectModel> locationClaims = getProperty(propertyId);
    if (locationClaims == null) {
      return Optional.empty();
    }
    return locationClaims.get(0).getValue();
  }

  private Optional<String> getHumanDescription(WikidataApi wikidataApi) {
    String description =
        Stream.of(
                getFirstClaimOfProperty(WikidataFields.COUNTRY_OF_CITIZENSHIP.label)
                    .flatMap(wikidataApi::getTitleOfWikidataId)
                    .orElse(""),
                getFirstClaimOfProperty(WikidataFields.BIRTHDAY.label)
                    .map(WikidataValue::toDateDescription)
                    .orElse(""))
            .filter(value -> !value.isBlank())
            .collect(Collectors.joining(", "));

    return Optional.of(description);
  }

  private Optional<String> getCountryDescription() {
    return getFirstClaimOfProperty(WikidataFields.COORDINATE_LOCATION.label)
        .map(WikidataValue::toLocationDescription);
  }

  private boolean isInstanceOf(WikidataItems human) {
    return getFirstClaimOfProperty(WikidataFields.INSTANCE_OF.label)
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
    return getFirstClaimOfProperty(WikidataFields.COORDINATE_LOCATION.label)
        .flatMap(WikidataValue::getCoordinate);
  }
}
