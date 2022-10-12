package com.odde.doughnut.services.externalApis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WikidataEntity {
  @Getter @Setter protected Map<String, List<WikidataClaimItem>> claims;

  public Optional<WikidataValue> getFirstClaimValue(String propertyId) {
    if (claims == null) {
      return Optional.empty();
    }
    List<WikidataClaimItem> listOfItems = claims.getOrDefault(propertyId, null);
    if (listOfItems == null || listOfItems.isEmpty()) {
      return Optional.empty();
    }
    return listOfItems.get(0).getValue();
  }
}
