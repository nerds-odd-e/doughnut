package com.odde.doughnut.services.httpQuery;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component
public class HttpClientAdapter {
  public HttpClientAdapter() {}

  public String getResponseString(URI uri) throws IOException, InterruptedException {
    return HttpClient.newBuilder()
        .build()
        .send(
            HttpRequest.newBuilder(uri).build(),
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        .body();
  }
}
