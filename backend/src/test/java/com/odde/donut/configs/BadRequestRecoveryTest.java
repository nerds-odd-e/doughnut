package com.odde.donut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;

class BadRequestRecoveryTest {
  @TempDir Path serverDirectory;

  private AnnotationConfigApplicationContext application;
  private WebServer server;
  private final AtomicInteger servletRequests = new AtomicInteger();

  @BeforeEach
  void startServer() {
    application = new AnnotationConfigApplicationContext();
    application.registerBean(Path.class, () -> serverDirectory);
    application.register(ServerConfiguration.class);
    application.refresh();
    server =
        application
            .getBean(TomcatServletWebServerFactory.class)
            .getWebServer(
                context ->
                    context
                        .addServlet(
                            "request-boundary",
                            new HttpServlet() {
                              @Override
                              protected void doGet(
                                  HttpServletRequest request, HttpServletResponse response)
                                  throws IOException {
                                servletRequests.incrementAndGet();
                                if (request.getRequestURI().equals("/json-error")) {
                                  response.setStatus(400);
                                  response.setContentType("application/json");
                                  response.getWriter().write("{\"error\":\"invalid input\"}");
                                } else if (request.getRequestURI().equals("/missing")) {
                                  response.sendError(404);
                                } else {
                                  response.getWriter().write("callback reached servlet");
                                }
                              }
                            })
                        .addMapping("/*"));
    server.start();
  }

  @AfterEach
  void stopServer() {
    try {
      if (server != null) server.destroy();
    } finally {
      if (application != null) application.close();
    }
  }

  @Test
  void callbackWithSmallCookieReachesServlet() throws Exception {
    HttpResponse<String> response = callbackWithCookie(4 * 1024);

    assertThat(response.statusCode(), equalTo(200));
    assertThat(response.body(), equalTo("callback reached servlet"));
    assertThat(servletRequests.get(), equalTo(1));
  }

  @Test
  void oversizedCookieIsRejectedBeforeServletDispatch() throws Exception {
    HttpResponse<String> response = callbackWithCookie(9 * 1024);

    assertThat(response.statusCode(), equalTo(400));
    assertThat(servletRequests.get(), equalTo(0));
    assertThat(response.body(), containsString("We could not process this request"));
    assertThat(response.body(), containsString("start a fresh login"));
    assertThat(response.body(), containsString("Private tab"));
    assertThat(
        response.body(),
        containsString("Settings &gt; Apps &gt; Safari &gt; Advanced &gt; Website Data"));
    assertThat(response.body(), containsString("only this affected site"));
    assertThat(response.body(), containsString("sign you out"));
    assertThat(response.body(), containsString("preferences and local search history"));
    assertThat(response.body(), containsString("<a href=\"/\">Go to the homepage</a>"));
    assertThat(
        response.body(),
        equalTo(
            new ClassPathResource("bad-request-recovery.html")
                .getContentAsString(StandardCharsets.UTF_8)));
    assertThat(
        response.headers().firstValue("content-type").orElseThrow(),
        equalTo("text/html;charset=UTF-8"));
    assertThat(response.headers().firstValue("location").isEmpty(), equalTo(true));
    assertThat(response.headers().firstValue("set-cookie").isEmpty(), equalTo(true));
    for (String forbidden :
        new String[] {
          "<script",
          "http-equiv",
          "src=",
          "href=\"http",
          "<link",
          "dummy-code",
          "dummy-state",
          "dummy-cookie",
          "searchKeyHistory",
          "a".repeat(100)
        }) {
      assertThat(response.body(), not(containsString(forbidden)));
    }
  }

  @Test
  void servletProducedBadRequestRemainsJson() throws Exception {
    HttpResponse<String> response = request("/json-error", 4 * 1024);

    assertThat(response.statusCode(), equalTo(400));
    assertThat(
        response.headers().firstValue("content-type").orElseThrow(),
        startsWith("application/json"));
    assertThat(response.body(), equalTo("{\"error\":\"invalid input\"}"));
  }

  @Test
  void otherErrorsKeepTheDefaultReport() throws Exception {
    HttpResponse<String> response = request("/missing", 0);

    assertThat(response.statusCode(), equalTo(404));
    assertThat(response.body(), containsString("HTTP Status 404"));
    assertThat(response.body(), not(containsString("Go to the homepage")));
  }

  private HttpResponse<String> callbackWithCookie(int cookieSize) throws Exception {
    return request("/login/oauth2/code/github?code=dummy-code&state=dummy-state", cookieSize);
  }

  private HttpResponse<String> request(String path, int cookieSize) throws Exception {
    try (HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()) {
      return client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:" + server.getPort() + path))
              .timeout(Duration.ofSeconds(10))
              .header("Cookie", "searchKeyHistory=dummy-cookie" + "a".repeat(cookieSize))
              .GET()
              .build(),
          HttpResponse.BodyHandlers.ofString());
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  @Import(BadRequestRecoveryConfiguration.class)
  static class ServerConfiguration {
    @Bean
    TomcatServletWebServerFactory webServerFactory(
        Path serverDirectory, BadRequestRecoveryConfiguration recoveryConfiguration) {
      TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
      factory.setBaseDirectory(serverDirectory.toFile());
      recoveryConfiguration.customize(factory);
      return factory;
    }
  }
}
