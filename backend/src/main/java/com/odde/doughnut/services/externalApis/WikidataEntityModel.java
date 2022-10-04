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

  private Optional<WikidataValue> getFirstClaimOfProperty(String wikidataId, String propertyId) {
    return getEntityItem(wikidataId).getFirstClaimOfProperty(propertyId);
  }

  public Optional<String> getDescription(String wikidataId) {
    if (getWikiClass(wikidataId).equals(Optional.of("Q5"))) {
      Optional<String> country =
          getFirstClaimOfProperty(wikidataId, "P27").map(WikidataValue::toCountryOfCitizenship);
      Optional<String> birthday =
          getFirstClaimOfProperty(wikidataId, "P569").map(WikidataValue::toDateDescription);
      return Optional.of(country.get() + birthday.get());
    } else {
      return getFirstClaimOfProperty(wikidataId, "P625").map(WikidataValue::toLocationDescription);
    }
  }

  private Optional<String> getWikiClass(String wikidataId) {
    return getFirstClaimOfProperty(wikidataId, "P31").map(WikidataValue::toWikiClass);
  }
}
