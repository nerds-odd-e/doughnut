package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.json.WikiDataDto;
import com.odde.doughnut.services.HttpClientAdapter;
import com.odde.doughnut.services.WikiDataService;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RestWikidataController {
  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private HttpClientAdapter httpClientAdapter;

  public RestWikidataController(
      TestabilitySettings testabilitySettings, HttpClientAdapter httpClientAdapter) {
    this.testabilitySettings = testabilitySettings;
    this.httpClientAdapter = httpClientAdapter;
  }

  @GetMapping("/wikidata/{wikiDataId}")
  public WikiDataDto fetchWikiDataByID(@PathVariable("wikiDataId") String wikiDataId)
      throws IOException, InterruptedException {
    return getWikiDataService().fetchWikiData(wikiDataId);
  }

  private WikiDataService getWikiDataService() {
    return new WikiDataService(httpClientAdapter, testabilitySettings.getWikidataServiceUrl());
  }
}
