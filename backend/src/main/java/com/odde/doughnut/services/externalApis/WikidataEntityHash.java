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
    return getEntity(wikidataId)
        .flatMap(x -> x.getFirstClaimOfProperty(WikidataFields.COORDINATE_LOCATION.label))
        .flatMap(WikidataValue::getCoordinate);
  }

  public boolean isBook(String wikidataId) {
    return getEntity(wikidataId)
        .flatMap(x -> x.getFirstClaimOfProperty(WikidataFields.INSTANCE_OF.label))
        .map(WikidataValue::toWikiClass)
        .equals(Optional.of(WikidataItems.BOOK.label));
  }

  public Optional<String> getDescription(WikidataService service, Optional<WikidataEntity> entity) {
    if (entity
        .flatMap(x -> x.getFirstClaimOfProperty(WikidataFields.INSTANCE_OF.label))
        .map(WikidataValue::toWikiClass)
        .equals(Optional.of(WikidataItems.HUMAN.label))) {
      return getHumanDescription(service, entity);
    }
    return getCountryDescription(entity);
  }

  private Optional<String> getCountryDescription(Optional<WikidataEntity> entity) {
    return entity
        .flatMap(x -> x.getFirstClaimOfProperty(WikidataFields.COORDINATE_LOCATION.label))
        .map(WikidataValue::toLocationDescription);
  }

  private Optional<String> getHumanDescription(
      WikidataService service, Optional<WikidataEntity> entity) {
    String description =
        Stream.of(
                entity
                    .flatMap(
                        x1 ->
                            x1.getFirstClaimOfProperty(WikidataFields.COUNTRY_OF_CITIZENSHIP.label))
                    .flatMap(service::getTitle)
                    .orElse(""),
                entity
                    .flatMap(x -> x.getFirstClaimOfProperty(WikidataFields.BIRTHDAY.label))
                    .map(WikidataValue::toDateDescription)
                    .orElse(""))
            .filter(value -> !value.isBlank())
            .collect(Collectors.joining(", "));

    return Optional.of(description);
  }
}
