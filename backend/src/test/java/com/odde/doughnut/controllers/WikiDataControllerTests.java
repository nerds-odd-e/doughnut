package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.models.WikiDataModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

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

    ObjectMapper mapper = new ObjectMapper();
    String result = mapper.writeValueAsString(resultObj);
    System.out.println(resultObj);
    assertThat(result, containsString("TDD"));
    assertThat(resultObj.entities.containsKey(searchId), is(true));
  }

}
