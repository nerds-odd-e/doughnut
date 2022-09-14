package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.json.WikidataEntity;
import com.odde.doughnut.entities.json.WikidataSearchEntity;
import com.odde.doughnut.services.HttpClientAdapter;
import com.odde.doughnut.services.WikidataService;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
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

  @GetMapping("/wikidata/{wikidataId}")
  public WikidataEntity fetchWikidataByID(@PathVariable("wikidataId") String wikidataId)
      throws InterruptedException, BindException {
    try {
      return getWikidataService().fetchWikidata(wikidataId);
    } catch (IOException e) {
      throw buildWikidataServiceNotAvailableException(wikidataId, "wikidataId");
    }
  }

  @GetMapping("/wikidata/search/{search}")
  public List<WikidataSearchEntity> searchWikidata(@PathVariable("search") String search)
      throws InterruptedException, BindException {
    try {
      return getWikidataService().fetchWikidataByQuery(search);
    } catch (IOException e) {
      throw buildWikidataServiceNotAvailableException(search, "search");
    }
  }

  private WikidataService getWikidataService() {
    return new WikidataService(httpClientAdapter, testabilitySettings.getWikidataServiceUrl());
  }

  private BindException buildWikidataServiceNotAvailableException(
      String wikidataId, String wikidataId1) throws BindException {
    BindingResult bindingResult = new BeanPropertyBindingResult(wikidataId, wikidataId1);
    bindingResult.rejectValue(null, "error.error", "The wikidata service is not available");
    return new BindException(bindingResult);
  }
}
