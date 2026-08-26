package com.odde.donut.services.wikidataApis.thirdPartyEntities;

import com.odde.donut.controllers.dto.WikidataSearchEntity;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WikidataSearchResult {
  public List<Map<String, Object>> search;

  public List<WikidataSearchEntity> getWikidataSearchEntities() {
    return search.stream().map(WikidataSearchEntity::new).collect(Collectors.toList());
  }
}
