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
    HttpResponse<String> response =
        HttpClient.newBuilder()
            .build()
            .send(
                HttpRequest.newBuilder(uri)
                    .header(
                        "User-Agent",
                        "Doughnut/1.0 (https://github.com/nerds-odd-e/doughnut; contact@odd-e.com)")
                    .header("Accept", "application/json")
                    .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

    if (response.statusCode() >= 400) {
      throw new IOException("HTTP error: " + response.statusCode() + " - " + response.body());
    }

    return response.body();
  }
}
