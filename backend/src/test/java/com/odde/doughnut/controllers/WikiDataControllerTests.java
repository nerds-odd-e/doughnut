package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.entities.json.WikiDataDto;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;

class WikiDataControllerTests {

  RestWikidataController controller;

  @BeforeEach
  void Setup() {
    controller = new RestWikidataController();
  }

  // integration test for wikidata api
  // Q423392 is id for article TDD
  void ShouldBeAbleToConnectToWikiDataApi() throws IOException, InterruptedException {
    String searchId = "Q423392";
    WikiDataDto resultObj = controller.fetchWikiDataDto(searchId);
    assertThat(resultObj.WikiDataId, equalTo(searchId));
    assertThat(resultObj.WikiDataTitleInEnglish, equalTo("TDD"));
  }
}
