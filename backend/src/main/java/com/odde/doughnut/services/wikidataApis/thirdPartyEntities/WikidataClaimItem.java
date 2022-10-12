package com.odde.doughnut.services.wikidataApis.thirdPartyEntities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.odde.doughnut.services.wikidataApis.WikidataValue;
import java.util.Optional;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WikidataClaimItem {
  static String VALUE_KEY = "value";
  static String VALUE_TYPE_KEY = "type";

  public WikidataMainsnak mainsnak;

  @JsonIgnore
  public Optional<WikidataValue> getValue() {
    if (mainsnak == null || mainsnak.getDatavalue() == null) {
      return Optional.empty();
    }
    return Optional.of(new WikidataValue(mainsnak));
  }
}
