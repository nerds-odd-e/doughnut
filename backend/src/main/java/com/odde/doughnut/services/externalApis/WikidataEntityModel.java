package com.odde.doughnut.services.externalApis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.odde.doughnut.entities.Coordinate;
import com.odde.doughnut.services.WikidataService;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WikidataEntityModel {
  private Map<String, WikidataEntityItemModel> entities;

  public Optional<WikidataValue> getFirstClaimOfProperty(
      String wikidataId, WikidataFields propertyId) {
    if (entities == null || !entities.containsKey(wikidataId)) {
      return Optional.empty();
    }
    return entities.get(wikidataId).getFirstClaimOfProperty(propertyId.label);
  }

  public Optional<Coordinate> getCoordinate(String wikidataId) {
    return getFirstClaimOfProperty(wikidataId, WikidataFields.COORDINATE_LOCATION)
        .flatMap(WikidataValue::getCoordinate);
  }

  public boolean isBook(String wikidataId) {
    return getWikiClass(wikidataId).equals(Optional.of(WikidataItems.BOOK.label));
  }

  public Optional<String> getDescription(WikidataService service, String wikidataId) {

    Optional<String> description;

    if (getWikiClass(wikidataId).equals(Optional.of(WikidataItems.HUMAN.label))) {
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

  private Optional<String> getWikiClass(String wikidataId) {
    return getFirstClaimOfProperty(wikidataId, WikidataFields.INSTANCE_OF)
        .map(WikidataValue::toWikiClass);
  }
}
