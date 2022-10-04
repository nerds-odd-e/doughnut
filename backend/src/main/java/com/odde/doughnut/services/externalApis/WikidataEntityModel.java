package com.odde.doughnut.services.externalApis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import java.util.Optional;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WikidataEntityModel {
  private Map<String, WikidataEntityItemModel> entities;

  private WikidataEntityItemModel getEntityItem(String wikidataId) {
    return entities.get(wikidataId);
  }

  private Optional<WikidataValue> getFirstClaimOfProperty(String wikidataId, WikidataFields propertyId) {
    return getEntityItem(wikidataId).getFirstClaimOfProperty(propertyId.label);
  }

  public Optional<String> getDescription(String wikidataId) {

    Optional<String> description;

    if (getWikiClass(wikidataId).equals(Optional.of("Q5"))) {
      Optional<String> country = getFirstClaimOfProperty(wikidataId, WikidataFields.COUNTRY_OF_CITIZENSHIP).map(WikidataValue::toCountryOfCitizenship);
      Optional<String> birthday = getFirstClaimOfProperty(wikidataId, WikidataFields.BIRTHDAY).map(WikidataValue::toDateDescription);

      description = Optional.of(country.get() + birthday.get());

    } else {
      description = getFirstClaimOfProperty(wikidataId, WikidataFields.COORDINATE_LOCATION).map(WikidataValue::toLocationDescription);
    }

    return description;
  }

  private Optional<String> getWikiClass(String wikidataId) {
    return getFirstClaimOfProperty(wikidataId, WikidataFields.INSTANCE_OF).map(WikidataValue::toWikiClass);
  }

}
