package com.odde.doughnut.services.httpQuery;

import java.io.IOException;
import org.springframework.web.util.UriComponentsBuilder;

public record QueryBuilder(
    HttpClientAdapter httpClientAdapter, UriComponentsBuilder uriComponentsBuilder) {
  public QueryBuilder path(String s) {
    return new QueryBuilder(httpClientAdapter, uriComponentsBuilder.cloneBuilder().path(s));
  }

  public QueryBuilder queryParam(String name, Object value) {
    return new QueryBuilder(
        httpClientAdapter, uriComponentsBuilder.cloneBuilder().queryParam(name, value));
  }

  public QueryResult queryResult(Object... uriVariables) throws IOException, InterruptedException {
    return new QueryResult(
        httpClientAdapter.getResponseString(uriComponentsBuilder.build(uriVariables)));
  }
}
