package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.json.WikiDataDto;
import com.odde.doughnut.services.HttpClientAdapter;
import com.odde.doughnut.services.WikiDataService;
import java.io.IOException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RestWikidataController {
  private WikiDataService wikiDataService;

  public RestWikidataController(HttpClientAdapter httpClientAdapter) {
    this.wikiDataService = new WikiDataService(httpClientAdapter);
  }

  @GetMapping("/wikidata/{wikiDataId}")
  public WikiDataDto fetchWikiDataByID(@PathVariable("wikiDataId") String wikiDataId)
      throws IOException, InterruptedException {
    return wikiDataService.FetchWikiData(wikiDataId).GetInfoForWikiDataId(wikiDataId).processInfo();
  }
}
