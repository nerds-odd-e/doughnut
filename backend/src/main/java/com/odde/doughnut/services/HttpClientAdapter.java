package com.odde.doughnut.services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class HttpClientAdapter {
  public HttpClientAdapter() {}

  public String getResponseString(String url) throws IOException, InterruptedException {
    final HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url));
    HttpRequest request = builder.build();
    HttpResponse.BodyHandler<String> bodyHandler =
        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
    return HttpClient.newBuilder().build().send(request, bodyHandler).body();
  }
}
