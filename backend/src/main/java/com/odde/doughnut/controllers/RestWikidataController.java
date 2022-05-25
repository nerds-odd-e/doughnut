package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.json.WikiDataDto;
import com.odde.doughnut.models.LanguageValueModel;
import com.odde.doughnut.models.WikiDataInfo;
import com.odde.doughnut.models.WikiDataModel;
import com.odde.doughnut.services.WikiDataService;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RestWikidataController {
  @Autowired WikiDataService wikiDataService;

  public RestWikidataController(WikiDataService wikiDataService) {

    this.wikiDataService = wikiDataService;
  }

  @GetMapping("/wikidata/{wikiDataId}")
  public WikiDataDto fetchWikiDataDto(@PathVariable("wikiDataId") String wikiDataId)
      throws IOException, InterruptedException {
    WikiDataModel wikiModel = wikiDataService.FetchWikiData(wikiDataId);
    WikiDataDto returnDto = new WikiDataDto();
    if (wikiModel.entities.containsKey(wikiDataId)) {
      WikiDataInfo myInfo = wikiModel.entities.get(wikiDataId);
      LanguageValueModel englishTitlePair = myInfo.labels.get("en");
      returnDto.WikiDataId = wikiDataId;
      returnDto.WikiDataTitleInEnglish = englishTitlePair.value;
    }
    return returnDto;
  }
}
