package com.odde.doughnut.services.externalApis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WikidataEntityItemModel {
  private String type;
  private String id;
  Map<String, List<WikidataEntityItemObjectModel>> claims;

  List<WikidataEntityItemObjectModel> getProperty(String propertyId) {
    if (claims == null) {
      return null;
    }
    return claims.getOrDefault(propertyId, null);
  }

  Map<String, Object> getFirstClaimOfProperty(String propertyId) {
    List<WikidataEntityItemObjectModel> locationClaims = getProperty(propertyId);
    if (locationClaims == null) {
      return null;
    }
    return locationClaims.get(0).getData();
  }
}
