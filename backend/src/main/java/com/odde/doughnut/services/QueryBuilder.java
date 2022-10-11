package com.odde.doughnut.services;

import java.io.IOException;
import java.net.URI;
import org.springframework.web.util.UriComponentsBuilder;

public record QueryBuilder(
    HttpClientAdapter httpClientAdapter, UriComponentsBuilder uriComponentsBuilder) {
  public String query(URI uri) throws IOException, InterruptedException {
    return httpClientAdapter.getResponseString(uri);
  }

  public QueryBuilder path(String s) {
    return new QueryBuilder(httpClientAdapter, uriComponentsBuilder.cloneBuilder().path(s));
  }

  public QueryBuilder queryParam(String name, Object value) {
    return new QueryBuilder(
        httpClientAdapter, uriComponentsBuilder.cloneBuilder().queryParam(name, value));
  }

  public String query(Object... uriVariables) throws IOException, InterruptedException {
    return httpClientAdapter.getResponseString(uriComponentsBuilder.build(uriVariables));
  }

  public QueryResult queryResult() throws IOException, InterruptedException {
    return new QueryResult(query());
  }
}
