package com.odde.doughnut.services.wikidataApis.thirdPartyEntities;

import com.odde.doughnut.services.wikidataApis.WikidataEntityModel;
import java.util.Map;
import java.util.Optional;
import lombok.Setter;

public class WikidataEntityHash {
  @Setter private Map<String, WikidataEntity> entities;

  public Optional<WikidataEntityModel> getEntityModel(String wikidataId) {
    if (entities == null || !entities.containsKey(wikidataId)) {
      return Optional.empty();
    }
    return Optional.of(new WikidataEntityModel(entities.get(wikidataId)));
  }
}
