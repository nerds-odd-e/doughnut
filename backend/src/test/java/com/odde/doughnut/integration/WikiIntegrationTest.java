package com.odde.doughnut.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.models.WikiDataInfo;
import com.odde.doughnut.models.WikiDataModel;
import com.odde.doughnut.services.WikiDataService;
import java.io.IOException;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WikiIntegrationTest {
  WikiDataService service;

  @BeforeEach
  void Setup() {
    service = new WikiDataService();
  }

  @Test
  void ShouldBeAbleToConnectToWikiDataApi() throws IOException, InterruptedException {
    String searchId = "Q423392";

    WikiDataModel result = service.FetchWikiData(searchId);
    WikiDataInfo myInfo = result.entities.get(searchId);
    assertThat(result, notNullValue());
    assertThat(myInfo.title, equalTo(searchId));
    assertThat(myInfo.labels.containsKey("en"), is(true));
    assertThat(myInfo.labels.get("en").value.toUpperCase(Locale.ROOT), equalTo("TDD"));
    assertThat(myInfo.sitelinks.get("enwiki").url, equalTo("https://en.wikipedia.org/wiki/TDD"));
  }
}
