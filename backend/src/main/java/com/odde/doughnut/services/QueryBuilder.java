package com.odde.doughnut.services;

import java.io.IOException;
import java.net.URI;

public record QueryBuilder(HttpClientAdapter httpClientAdapter, String wikidataUrl) {

  public String query(URI uri) throws IOException, InterruptedException {
    return httpClientAdapter.getResponseString(uri);
  }
}
