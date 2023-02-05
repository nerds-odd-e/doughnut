package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.json.WikidataEntityData;
import com.odde.doughnut.entities.json.WikidataSearchEntity;
import com.odde.doughnut.services.HttpClientAdapter;
import com.odde.doughnut.services.WikidataService;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
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
      @PathVariable("wikidataId") String wikidataId) throws BindException {
    try {
      return wikidataService.wrapWikidataIdWithApi(wikidataId).fetchWikidataEntityData();
    } catch (IOException e) {
      throw buildWikidataServiceNotAvailableException("wikidataId");
    }
  }

  @GetMapping("/search/{search}")
  public List<WikidataSearchEntity> searchWikidata(@PathVariable("search") String search)
      throws InterruptedException, BindException {
    try {
      return wikidataService.searchWikidata(search);
    } catch (IOException e) {
      throw buildWikidataServiceNotAvailableException("search");
    }
  }

  private BindException buildWikidataServiceNotAvailableException(String fieldName) {
    BindingResult bindingResult = new BeanPropertyBindingResult(null, fieldName);
    bindingResult.rejectValue(null, "error.error", "The wikidata service is not available");
    return new BindException(bindingResult);
  }
}
