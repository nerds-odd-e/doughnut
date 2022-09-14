package com.odde.doughnut.services.externalApis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WikidataEntityModel {
  private Map<String, WikidataEntityItemModel> entities;
  private Number success;

  private List<WikidataEntityItemObjectModel> getLocationClaims(
      String wikidataId, String locationId) {
    WikidataEntityItemModel entityItem = getEntities().get(wikidataId);
    if (entityItem.getClaims() == null) {
      return null;
    }
    if (entityItem.getClaims().containsKey(locationId)) {
      return entityItem.getClaims().get(locationId);
    }

    return null;
  }

  private Map<String, Object> getStringObjectMap(String wikidataId, String locationId) {
    if (!getEntities().containsKey(wikidataId)) return null;
    List<WikidataEntityItemObjectModel> locationClaims = getLocationClaims(wikidataId, locationId);
    if (locationClaims == null) {
      return null;
    }
    Map<String, Object> locationValue = locationClaims.get(0).getData();
    return locationValue;
  }

  public String getLocationDescription(String wikidataId) {
    Map<String, Object> locationValue = getStringObjectMap(wikidataId, "P625");
    if (locationValue == null) return null;

    return "Location: "
        + locationValue.get("latitude").toString()
        + "'N, "
        + locationValue.get("longitude").toString()
        + "'E";
  }
}
