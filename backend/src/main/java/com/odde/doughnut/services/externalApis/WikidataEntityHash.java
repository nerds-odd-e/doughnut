package com.odde.doughnut.services.externalApis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import java.util.Optional;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WikidataEntityHash {
  @Setter private Map<String, WikidataEntity> entities;

  public Optional<WikidataEntityModelOfProperties> getEntityModel(String wikidataId) {
    if (entities == null || !entities.containsKey(wikidataId)) {
      return Optional.empty();
    }
    return Optional.of(new WikidataEntityModelOfProperties(entities.get(wikidataId)));
  }
}
