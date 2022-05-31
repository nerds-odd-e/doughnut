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

    private String getEntityDataJsonString(String entityId, String entityTitle, String enwikiJson) {
      return "{\"entities\":{\""
          + entityId
          + "\":{\"labels\":{\"en\":{\"language\":\"en\",\"value\":\""
          + entityTitle
          + "\"}},\"sitelinks\":{"
          + enwikiJson
          + "}}}}";
    }

    @Test
    void shouldFetchInfoViaWikidataApi() throws IOException, InterruptedException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(getEntityDataJsonString("Q1", "Mohawk", ""));
      controller.fetchWikiDataByID("Q1");
      Mockito.verify(httpClientAdapter)
          .getResponseString("https://www.wikidata.org/wiki/Special:EntityData/Q1.json");
    }

    @Test
    void shouldParseAndGetTheInfo() throws IOException, InterruptedException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(getEntityDataJsonString("Q1", "Mohawk", ""));
      WikiDataDto result = controller.fetchWikiDataByID("Q1");
      assertThat(result.WikiDataTitleInEnglish, equalTo("Mohawk"));
    }

    @Test
    void shouldRetrieveTheEnglishWikipediaLinkIfExists() throws IOException, InterruptedException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(
              getEntityDataJsonString(
                  "Q13339",
                  "Mohawk",
                  "\"enwiki\":{\"site\":\"enwiki\",\"title\":\"Mohawk language\",\"badges\":[],\"url\":\"https://en.wikipedia.org/wiki/Mohawk_language\"}"));
      WikiDataDto result = controller.fetchWikiDataByID("Q13339");
      assertThat(
          result.WikipediaEnglishUrl, equalTo("https://en.wikipedia.org/wiki/Mohawk_language"));
    }

    @Test
    void shouldReturnEmptyIfEnglishWikipediaLinkNotExist()
        throws IOException, InterruptedException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(getEntityDataJsonString("Q13339", "Blah", ""));
      WikiDataDto result = controller.fetchWikiDataByID("Q13339");
      assertThat(StringUtils.isBlank(result.WikipediaEnglishUrl), is(true));
    }
  }
}
