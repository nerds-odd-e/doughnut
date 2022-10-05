package com.odde.doughnut.services.externalApis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import org.thymeleaf.util.StringUtils;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WikidataEntityModel {
  private Map<String, WikidataEntityItemModel> entities;

  private WikidataEntityItemModel getEntityItem(String wikidataId) {
    return entities.get(wikidataId);
  }

  private Optional<WikidataValue> getFirstClaimOfProperty(
      String wikidataId, WikidataFields propertyId) {
    return getEntityItem(wikidataId).getFirstClaimOfProperty(propertyId.label);
  }

  public Optional<String> getDescription(String wikidataId) {

    Optional<String> description;

    if (getWikiClass(wikidataId).equals(Optional.of(WikidataItems.HUMAN.label))) {
      Optional<String> country =
          getFirstClaimOfProperty(wikidataId, WikidataFields.COUNTRY_OF_CITIZENSHIP)
              .map(
                  wikiId -> {
                    if (StringUtils.equals("Q865", wikiId.toWikiClass())) {
                      return "Taiwan";
                    } else {
                      return "";
                    }
                  });
      Optional<String> birthday =
          getFirstClaimOfProperty(wikidataId, WikidataFields.BIRTHDAY)
              .map(WikidataValue::toDateDescription);
      // Add spacing between birthday and country only if country is not empty
      Optional<String> countryString =
          StringUtils.isEmpty(country.get()) ? Optional.of("") : Optional.of(country.get() + ", ");
      description = Optional.of(countryString.get() + birthday.get());

    } else {
      description =
          getFirstClaimOfProperty(wikidataId, WikidataFields.COORDINATE_LOCATION)
              .map(WikidataValue::toLocationDescription);
    }

    return description;
  }

  private Optional<String> getWikiClass(String wikidataId) {
    return getFirstClaimOfProperty(wikidataId, WikidataFields.INSTANCE_OF)
        .map(WikidataValue::toWikiClass);
  }
}
