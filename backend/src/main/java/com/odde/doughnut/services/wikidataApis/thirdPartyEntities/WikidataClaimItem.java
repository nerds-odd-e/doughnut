package com.odde.doughnut.services.wikidataApis.thirdPartyEntities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.services.wikidataApis.WikidataValue;
import java.util.Optional;
import lombok.Data;

@Data
public class WikidataClaimItem {

  public WikidataMainsnak mainsnak;

  @JsonIgnore
  public Optional<WikidataValue> toWikidataValue() {
    return Optional.of(new WikidataValue(mainsnak.datavalue));
  }
}
