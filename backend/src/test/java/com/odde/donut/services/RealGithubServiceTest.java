package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class RealGithubServiceTest {
  final RealGithubService service = new RealGithubService();
  final HttpClient httpClient = mock(HttpClient.class);

  @BeforeEach
  void setUp() {
    service.httpClient = httpClient;
    ReflectionTestUtils.setField(service, "githubForIssuesRepo", "owner/repo");
    ReflectionTestUtils.setField(service, "githubForIssuesToken", "test-token");
  }

  @SuppressWarnings("unchecked")
  private HttpResponse<String> mockResponse(int statusCode, String body) {
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(statusCode);
    when(response.body()).thenReturn(body);
    return response;
  }

  private static String bodyAsString(HttpRequest request) {
    HttpRequest.BodyPublisher bodyPublisher = request.bodyPublisher().orElseThrow();
    HttpResponse.BodySubscriber<String> bodySubscriber =
        HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
    bodyPublisher.subscribe(
        new Flow.Subscriber<>() {
          @Override
          public void onSubscribe(Flow.Subscription subscription) {
            bodySubscriber.onSubscribe(subscription);
          }

          @Override
          public void onNext(ByteBuffer item) {
            bodySubscriber.onNext(List.of(item));
          }

          @Override
          public void onError(Throwable throwable) {
            bodySubscriber.onError(throwable);
          }

          @Override
          public void onComplete() {
            bodySubscriber.onComplete();
          }
        });
    return bodySubscriber.getBody().toCompletableFuture().join();
  }

  @Test
  void closeIssueAsCompletedSendsPatchWithCompletedClosurePayload()
      throws IOException, InterruptedException {
    HttpResponse<String> response = mockResponse(200, "{}");
    when(httpClient.<String>send(any(), any())).thenReturn(response);

    service.closeIssueAsCompleted(42);

    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
    verify(httpClient).send(requestCaptor.capture(), any());
    HttpRequest request = requestCaptor.getValue();
    assertThat(request.uri().getPath(), equalTo("/repos/owner/repo/issues/42"));
    assertThat(request.method(), equalTo("PATCH"));
    assertThat(
        bodyAsString(request), equalTo("{\"state\":\"closed\",\"state_reason\":\"completed\"}"));
  }

  @Test
  void closeIssueAsCompletedSucceedsWhenGithubReportsSuccess()
      throws IOException, InterruptedException {
    HttpResponse<String> response = mockResponse(200, "{}");
    when(httpClient.<String>send(any(), any())).thenReturn(response);

    service.closeIssueAsCompleted(42);

    verify(httpClient).send(any(), any());
  }

  @Test
  void closeIssueAsCompletedThrowsWhenGithubReportsFailure()
      throws IOException, InterruptedException {
    HttpResponse<String> response = mockResponse(404, "{\"message\":\"Not Found\"}");
    when(httpClient.<String>send(any(), any())).thenReturn(response);

    IOException exception =
        assertThrows(IOException.class, () -> service.closeIssueAsCompleted(42));
    assertThat(
        exception.getMessage(),
        equalTo("GitHub API returned HTTP 404: {\"message\":\"Not Found\"}"));
  }
}
