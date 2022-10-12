package com.odde.doughnut.services.wikidataApis.thirdPartyEntities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.services.wikidataApis.WikidataValue;
import java.util.Optional;
import lombok.Data;

@Data
public class WikidataClaimItem {

  public WikidataMainsnak mainsnak;

  @JsonIgnore
  public Optional<WikidataValue> getValue() {
    if (mainsnak == null || mainsnak.getDatavalue() == null) {
      return Optional.empty();
    }
    return Optional.of(new WikidataValue(mainsnak.datavalue));
  }
}
