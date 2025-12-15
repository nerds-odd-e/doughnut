package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.WikidataEntityData;
import com.odde.doughnut.controllers.dto.WikidataSearchEntity;
import com.odde.doughnut.exceptions.WikidataServiceErrorException;
import com.odde.doughnut.services.WikidataService;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/wikidata")
public class WikidataController {

  private final WikidataService wikidataService;

  public WikidataController(WikidataService wikidataService) {
    this.wikidataService = wikidataService;
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

  @GetMapping("/search")
  public List<WikidataSearchEntity> searchWikidata(@RequestParam("search") String search)
      throws InterruptedException {
    try {
      return wikidataService.searchWikidata(search);
    } catch (IOException e) {
      throw new WikidataServiceErrorException(
          "The wikidata service is not available", HttpStatus.NOT_FOUND);
    }
  }
}
