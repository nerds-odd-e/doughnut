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
  @Setter private Map<String, WikidataEntityItemModel> entities;

  public Optional<WikidataValue> getFirstClaimOfProperty(
      String wikidataId, WikidataFields propertyId) {
    return getEntity(wikidataId).flatMap(x -> x.getFirstClaimOfProperty(propertyId.label));
  }

  private Optional<WikidataEntityItemModel> getEntity(String wikidataId) {
    if (entities == null || !entities.containsKey(wikidataId)) {
      return Optional.empty();
    }
    return Optional.of(entities.get(wikidataId));
  }

  public Optional<Coordinate> getCoordinate(String wikidataId) {
    return getFirstClaimOfProperty(wikidataId, WikidataFields.COORDINATE_LOCATION)
        .flatMap(WikidataValue::getCoordinate);
  }

  public boolean isBook(String wikidataId) {
    return getFirstClaimOfProperty(wikidataId, WikidataFields.INSTANCE_OF)
        .map(WikidataValue::toWikiClass)
        .equals(Optional.of(WikidataItems.BOOK.label));
  }

  public Optional<String> getDescription(WikidataService service, String wikidataId) {

    Optional<String> description;

    if (getFirstClaimOfProperty(wikidataId, WikidataFields.INSTANCE_OF)
        .map(WikidataValue::toWikiClass)
        .equals(Optional.of(WikidataItems.HUMAN.label))) {
      description = getHumanDescription(service, wikidataId);
    } else {
      description = getCountryDescription(wikidataId);
    }

    return description;
  }

  private Optional<String> getCountryDescription(String wikidataId) {
    return getFirstClaimOfProperty(wikidataId, WikidataFields.COORDINATE_LOCATION)
        .map(WikidataValue::toLocationDescription);
  }

  private Optional<String> getHumanDescription(WikidataService service, String wikidataId) {
    String description =
        Stream.of(
                getFirstClaimOfProperty(wikidataId, WikidataFields.COUNTRY_OF_CITIZENSHIP)
                    .flatMap(service::getTitle)
                    .orElse(""),
                getFirstClaimOfProperty(wikidataId, WikidataFields.BIRTHDAY)
                    .map(WikidataValue::toDateDescription)
                    .orElse(""))
            .filter(value -> !value.isBlank())
            .collect(Collectors.joining(", "));

    return Optional.of(description);
  }
}
