package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.models.WikiDataModel;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WikiDataControllerTests {

  RestWikidataController controller;

  @BeforeEach
  void Setup() {
    controller = new RestWikidataController();
  }

  @Test
  void ShouldBeAbleToConnectToWikiDataApi() throws IOException, InterruptedException {
    String searchId = "Q423392";
    WikiDataModel resultObj = controller.fetchWikidata(searchId);
    assertThat(resultObj.entities.containsKey(searchId), is(true));
  }
}
