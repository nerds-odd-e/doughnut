package com.odde.doughnut.services.externalApis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.odde.doughnut.entities.Coordinate;
import com.odde.doughnut.services.WikidataService;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WikidataEntityHash {
  @Setter private Map<String, WikidataEntity> entities;

  public Optional<WikidataEntity> getEntity(String wikidataId) {
    if (entities == null || !entities.containsKey(wikidataId)) {
      return Optional.empty();
    }
    return Optional.of(entities.get(wikidataId));
  }

  public Optional<Coordinate> getCoordinate(String wikidataId) {
    return getEntity(wikidataId).flatMap(this::getCoordinateX);
  }

  public Optional<Coordinate> getCoordinateX(WikidataEntity entity) {
    return entity
        .getFirstClaimOfProperty(WikidataFields.COORDINATE_LOCATION.label)
        .flatMap(WikidataValue::getCoordinate);
  }

  public Optional<String> getDescription(WikidataService service, WikidataEntity entity) {
    if (isInstanceOf(entity, WikidataItems.HUMAN)) {
      return getHumanDescription(service, entity);
    }
    return getCountryDescription(entity);
  }

  private boolean isInstanceOf(WikidataEntity entity, WikidataItems human) {
    return entity
        .getFirstClaimOfProperty(WikidataFields.INSTANCE_OF.label)
        .map(WikidataValue::toWikiClass)
        .equals(Optional.of(human.label));
  }

  private Optional<String> getCountryDescription(WikidataEntity entity) {
    return entity
        .getFirstClaimOfProperty(WikidataFields.COORDINATE_LOCATION.label)
        .map(WikidataValue::toLocationDescription);
  }

  private Optional<String> getHumanDescription(WikidataService service, WikidataEntity entity) {
    String description =
        Stream.of(
                entity
                    .getFirstClaimOfProperty(WikidataFields.COUNTRY_OF_CITIZENSHIP.label)
                    .flatMap(service::getTitle)
                    .orElse(""),
                entity
                    .getFirstClaimOfProperty(WikidataFields.BIRTHDAY.label)
                    .map(WikidataValue::toDateDescription)
                    .orElse(""))
            .filter(value -> !value.isBlank())
            .collect(Collectors.joining(", "));

    return Optional.of(description);
  }
}
