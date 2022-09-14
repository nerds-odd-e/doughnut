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

  private Optional<Map<String, Object>> getFirstClaimOfProperty(
      String wikidataId, String propertyId) {
    return Optional.ofNullable(getEntityItem(wikidataId).getFirstClaimOfProperty(propertyId));
  }

  public Optional<String> getLocationDescription(String wikidataId) {
    return getFirstClaimOfProperty(wikidataId, "P625")
        .map(
            locationValue ->
                "Location: "
                    + locationValue.get("latitude").toString()
                    + "'N, "
                    + locationValue.get("longitude").toString()
                    + "'E");
  }
}
