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

class WikiDataControllerTests {

  RestWikidataController controller;
  @Mock HttpClientAdapter httpClientAdapter;

  @BeforeEach
  void Setup() {
    MockitoAnnotations.openMocks(this);
    controller = new RestWikidataController(httpClientAdapter);
  }

  @Nested
  class FetchWikiData {

    private String getJsonString(String mohawk, String enwiki) {
      return "{\"entities\":{\"Q13339\":{\"title\":\"Q13339\",\"labels\":{\"en\":{\"language\":\"en\",\"value\":\""
          + mohawk
          + "\"}},\"sitelinks\":{"
          + enwiki
          + "}}}}";
    }

    @Test
    void GivenSearchIdHasValue_ShouldBeAbleToGetWikiDataTitleAndId()
        throws IOException, InterruptedException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(getJsonString("Mohawk", ""));
      WikiDataDto resultObj = controller.fetchWikiDataDto("Q13339");
      assertThat(resultObj.WikiDataId, equalTo("Q13339"));
      assertThat(resultObj.WikiDataTitleInEnglish, equalTo("Mohawk"));
    }

    @Test
    void GivenSearchIdHasWikipediaEnglishLink_ShouldBeAbleToRetrieveLink()
        throws IOException, InterruptedException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(
              getJsonString(
                  "Mohawk",
                  "\"enwiki\":{\"site\":\"enwiki\",\"title\":\"Mohawk language\",\"badges\":[],\"url\":\"https://en.wikipedia.org/wiki/Mohawk_language\"}"));
      WikiDataDto resultObj = controller.fetchWikiDataDto("Q13339");
      assertThat(
          resultObj.WikipediaEnglishUrl, equalTo("https://en.wikipedia.org/wiki/Mohawk_language"));
    }

    @Test
    void GivenSearchIdHasNoWikipediaEnglishLink_ShouldReturnEmptyLink()
        throws IOException, InterruptedException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(getJsonString("Mohawk", ""));
      WikiDataDto resultObj = controller.fetchWikiDataDto("Q13339");
      assertThat(StringUtils.isBlank(resultObj.WikipediaEnglishUrl), is(true));
    }
  }
}
