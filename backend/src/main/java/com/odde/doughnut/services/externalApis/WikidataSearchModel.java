package com.odde.doughnut.services.externalApis;

import com.odde.doughnut.entities.json.WikidataSearchEntity;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WikidataSearchModel {
  public List<Map<String, Object>> search;

  public List<WikidataSearchEntity> getWikidataSearchEntities() {
    return search.stream().map(WikidataSearchEntity::new).collect(Collectors.toList());
  }
}
