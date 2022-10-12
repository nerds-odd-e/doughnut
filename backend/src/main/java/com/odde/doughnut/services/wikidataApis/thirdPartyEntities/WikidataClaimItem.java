package com.odde.doughnut.services.wikidataApis.thirdPartyEntities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.services.wikidataApis.WikidataValue;
import lombok.Data;

@Data
public class WikidataClaimItem {

  public WikidataMainsnak mainsnak;

  @JsonIgnore
  public WikidataValue toWikidataValue() {
    return new WikidataValue(mainsnak.datavalue);
  }
}
