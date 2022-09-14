package com.odde.doughnut.services.externalApis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WikidataEntityItemModel {
  private String type;
  private String id;
  Map<String, List<WikidataEntityItemObjectModel>> claims;
}
