package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.json.WikidataEntityData;
import com.odde.doughnut.controllers.json.WikidataSearchEntity;
import com.odde.doughnut.exceptions.WikidataServiceErrorException;
import com.odde.doughnut.services.WikidataService;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/wikidata")
public class RestWikidataController {

  WikidataService wikidataService;

  public RestWikidataController(
      TestabilitySettings testabilitySettings, HttpClientAdapter httpClientAdapter) {
    wikidataService =
        new WikidataService(httpClientAdapter, testabilitySettings.getWikidataServiceUrl());
  }

  @GetMapping("/entity-data/{wikidataId}")
  public Optional<WikidataEntityData> fetchWikidataEntityDataByID(
      @PathVariable("wikidataId") String wikidataId) {
    try {
      return wikidataService.wrapWikidataIdWithApi(wikidataId).fetchWikidataEntityData();
    } catch (IOException e) {
      throw new WikidataServiceErrorException(
          "The wikidata service is not available", HttpStatus.NOT_FOUND);
    }
  }

  @GetMapping("/search/{search}")
  public List<WikidataSearchEntity> searchWikidata(@PathVariable("search") String search)
      throws InterruptedException {
    try {
      return wikidataService.searchWikidata(search);
    } catch (IOException e) {
      throw new WikidataServiceErrorException(
          "The wikidata service is not available", HttpStatus.NOT_FOUND);
    }
  }
}
