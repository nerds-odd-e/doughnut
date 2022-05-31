package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;

import com.odde.doughnut.entities.json.WikiDataDto;
import com.odde.doughnut.services.HttpClientAdapter;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class RestWikiDataControllerTests {

  RestWikidataController controller;
  @Mock HttpClientAdapter httpClientAdapter;

  @BeforeEach
  void Setup() {
    MockitoAnnotations.openMocks(this);
    controller = new RestWikidataController(httpClientAdapter);
  }

  @Nested
  class FetchWikiData {

    private String getEntityDataJsonString(String entityTitle, String enwikiJson, String entityId) {
      return "{\"entities\":{\""
          + entityId
          + "\":{\"labels\":{\"en\":{\"language\":\"en\",\"value\":\""
          + entityTitle
          + "\"}},\"sitelinks\":{"
          + enwikiJson
          + "}}}}";
    }

    @Test
    void GivenSearchIdHasValue_ShouldBeAbleToGetWikiDataTitleAndId()
        throws IOException, InterruptedException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(getEntityDataJsonString("Mohawk", "", "Q13339"));
      WikiDataDto resultObj = controller.fetchWikiDataDto("Q13339");
      assertThat(resultObj.WikiDataTitleInEnglish, equalTo("Mohawk"));
      Mockito.verify(httpClientAdapter)
          .getResponseString("https://www.wikidata.org/wiki/Special:EntityData/Q13339.json");
    }

    @Test
    void GivenSearchIdHasWikipediaEnglishLink_ShouldBeAbleToRetrieveLink()
        throws IOException, InterruptedException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(
              getEntityDataJsonString(
                  "Mohawk",
                  "\"enwiki\":{\"site\":\"enwiki\",\"title\":\"Mohawk language\",\"badges\":[],\"url\":\"https://en.wikipedia.org/wiki/Mohawk_language\"}",
                  "Q13339"));
      WikiDataDto resultObj = controller.fetchWikiDataDto("Q13339");
      assertThat(
          resultObj.WikipediaEnglishUrl, equalTo("https://en.wikipedia.org/wiki/Mohawk_language"));
    }

    @Test
    void GivenSearchIdHasNoWikipediaEnglishLink_ShouldReturnEmptyLink()
        throws IOException, InterruptedException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(getEntityDataJsonString("Mohawk", "", "Q13339"));
      WikiDataDto resultObj = controller.fetchWikiDataDto("Q13339");
      assertThat(StringUtils.isBlank(resultObj.WikipediaEnglishUrl), is(true));
    }
  }
}
