package com.odde.doughnut.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.odde.doughnut.entities.FailureReport;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RealGithubService implements GithubService {
  @Value("${spring.github_for_issues.repo}")
  private String githubForIssuesRepo;

  @Value("${spring.github_for_issues.token}")
  private String githubForIssuesToken;

  @Override
  public String getIssueUrl(Integer issueNumber) {
    return "https://github.com/" + githubForIssuesRepo + "/issues/" + issueNumber;
  }

  @Override
  public Integer createGithubIssue(FailureReport failureReport)
      throws IOException, InterruptedException {
    FailureReport.GithubIssue githubIssue = failureReport.getGithubIssue();
    final String body =
        new com.odde.doughnut.configs.ObjectMapperConfig()
            .objectMapper()
            .writeValueAsString(githubIssue);
    Map<String, Object> map =
        apiRequestWithMapAsResult(
            "issues", (builder) -> builder.POST(BodyPublishers.ofString(body)));

    return (Integer) map.get("number");
  }

  @Override
  public List<Map<String, Object>> getOpenIssues() throws IOException, InterruptedException {
    return apiRequestWithArrayAsResult("issues?state=open", HttpRequest.Builder::GET);
  }

  @Override
  public void closeAllOpenIssues() throws IOException, InterruptedException {
    getOpenIssues().forEach(issue -> closeIssue((Integer) issue.get("number")));
  }

  @SneakyThrows
  private void closeIssue(Integer issueNumber) {
    apiRequest(
        "issues/" + issueNumber,
        builder -> builder.POST(BodyPublishers.ofString("{\"state\":\"closed\"}")));
  }

  private List<Map<String, Object>> apiRequestWithArrayAsResult(
      String action, Function<HttpRequest.Builder, HttpRequest.Builder> callback)
      throws IOException, InterruptedException {
    HttpResponse<String> response = apiRequest(action, callback);
    return new com.odde.doughnut.configs.ObjectMapperConfig()
        .objectMapper()
        .readValue(response.body(), new TypeReference<>() {});
  }

  private Map<String, Object> apiRequestWithMapAsResult(
      String action, Function<HttpRequest.Builder, HttpRequest.Builder> callback)
      throws IOException, InterruptedException {
    HttpResponse<String> response = apiRequest(action, callback);
    return new com.odde.doughnut.configs.ObjectMapperConfig()
        .objectMapper()
        .readValue(response.body(), new TypeReference<>() {});
  }

  private HttpResponse<String> apiRequest(
      String action, Function<HttpRequest.Builder, HttpRequest.Builder> callback)
      throws IOException, InterruptedException {
    assertToken();
    final HttpRequest.Builder builder =
        HttpRequest.newBuilder(
            URI.create("https://api.github.com/repos/" + githubForIssuesRepo + "/" + action));
    final HttpRequest.Builder builderWithRequest = callback.apply(builder);
    HttpRequest request =
        builderWithRequest
            .setHeader("Content-Type", "application/json")
            .setHeader("Accept", "application/vnd.github.v3+json")
            .setHeader("Authorization", "token " + githubForIssuesToken)
            .build();
    HttpResponse.BodyHandler<String> bodyHandler =
        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
    return HttpClient.newBuilder().build().send(request, bodyHandler);
  }

  private void assertToken() {
    if ("token_not_set".equals(githubForIssuesToken)) {
      throw new RuntimeException(
          "Github token is not set. To test this feature please make sure you have set the token in your development enironment.");
    }
  }
}
