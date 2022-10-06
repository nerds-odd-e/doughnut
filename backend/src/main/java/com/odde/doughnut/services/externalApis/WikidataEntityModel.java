package com.odde.doughnut.services.externalApis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.odde.doughnut.entities.Coordinate;
import com.odde.doughnut.services.WikidataService;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import org.thymeleaf.util.StringUtils;

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
        .map(v -> new Coordinate(v.getLatitude(), v.getLongitude()));
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
    Optional<String> description;
    Optional<String> country =
        getFirstClaimOfProperty(wikidataId, WikidataFields.COUNTRY_OF_CITIZENSHIP)
            .map(wikiId -> service.getCountry(wikiId));
    Optional<String> birthday =
        getFirstClaimOfProperty(wikidataId, WikidataFields.BIRTHDAY)
            .map(WikidataValue::toDateDescription);
    // Add spacing between birthday and country only if country is not empty
    Optional<String> countryString =
        StringUtils.isEmpty(country.get()) ? Optional.of("") : Optional.of(country.get() + ", ");
    description = Optional.of(countryString.get() + birthday.get());
    return description;
  }

  private Optional<String> getWikiClass(String wikidataId) {
    return getFirstClaimOfProperty(wikidataId, WikidataFields.INSTANCE_OF)
        .map(WikidataValue::toWikiClass);
  }
}
