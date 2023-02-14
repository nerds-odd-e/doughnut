package com.odde.doughnut.services.wikidataApis.thirdPartyEntities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.odde.doughnut.services.wikidataApis.WikidataValue;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WikidataEntity {
  @Getter @Setter protected Map<String, List<WikidataClaimItem>> claims;

  public Optional<WikidataValue> getFirstClaimValue(String propertyId) {
    return getClaimValues(propertyId).findFirst();
  }

  public Stream<WikidataValue> getClaimValues(String propertyId) {
    if (claims == null) {
      return Stream.empty();
    }
    List<WikidataClaimItem> listOfItems = claims.getOrDefault(propertyId, null);
    if (listOfItems == null || listOfItems.isEmpty()) {
      return Stream.empty();
    }
    return listOfItems.stream().map(WikidataClaimItem::toWikidataValue);
  }
}
