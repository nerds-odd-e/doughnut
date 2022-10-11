package com.odde.doughnut.services.externalApis;

import com.odde.doughnut.services.HttpClientAdapter;

public record WikidataApi(HttpClientAdapter httpClientAdapter, String wikidataUrl) {
  public HttpClientAdapter getHttpClientAdapter() {
    return httpClientAdapter;
  }

  public String getWikidataUrl() {
    return wikidataUrl;
  }
}
