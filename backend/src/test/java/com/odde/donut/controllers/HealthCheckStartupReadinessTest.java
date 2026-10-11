package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;

import com.odde.donut.DonutApplication;
import com.odde.donut.testability.UnitTestDatasource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * The embedded application started the way the E2E stack starts it: the deferred Flyway migration
 * runs on the ready event while the web server already answers HTTP.
 */
class HealthCheckStartupReadinessTest {
  private static final String READY_BODY = "OK\\. Active Profile: e2e\\. Commit: [0-9a-f]{40}";

  @Test
  void healthcheckWhileStartupMigrationIsRunningAndAfterStartupCompletes() throws Exception {
    try (var startup = new MigrationHeldStartup()) {
      startup.awaitMigrationEntered();
      HttpResponse<String> whileMigrating = startup.healthcheck();

      startup.releaseMigrationAndAwaitStartup();
      HttpResponse<String> afterStartup = startup.healthcheck();

      assertThat(whileMigrating.statusCode(), is(503));
      assertThat(whileMigrating.body(), equalTo("Starting"));
      assertThat(afterStartup.statusCode(), is(200));
      assertThat(afterStartup.body(), matchesPattern(READY_BODY));
      assertThat(
          startup.events,
          equalTo(
              List.of(
                  "migration entered",
                  "healthcheck 503",
                  "migration completed",
                  "startup completed",
                  "healthcheck 200")));
    }
  }

  /**
   * Starts the application on a real port against the Unit Test datasource and holds its deferred
   * Flyway migration open until released.
   */
  static class MigrationHeldStartup implements AutoCloseable {
    private static final long TIMEOUT_SECONDS = 180;

    final List<String> events = new CopyOnWriteArrayList<>();
    private final CountDownLatch migrationEntered = new CountDownLatch(1);
    private final CountDownLatch migrationReleased = new CountDownLatch(1);
    private final AtomicReference<ConfigurableApplicationContext> context = new AtomicReference<>();
    private final CompletableFuture<Void> startup;
    private final AtomicReference<Integer> port = new AtomicReference<>();
    private final HttpClient http = HttpClient.newHttpClient();

    MigrationHeldStartup() {
      SpringApplicationBuilder application =
          new SpringApplicationBuilder(DonutApplication.class)
              .listeners(
                  (ApplicationListener<WebServerInitializedEvent>)
                      started -> port.set(started.getWebServer().getPort()))
              .initializers(
                  starting -> {
                    context.set(starting);
                    starting.getBeanFactory().registerSingleton("migrationBarrier", barrier());
                  });
      startup =
          CompletableFuture.runAsync(
              () -> {
                application.run(
                    "--spring.profiles.active=e2e",
                    "--server.port=0",
                    "--" + UnitTestDatasource.URL_PROPERTY);
                events.add("startup completed");
              });
    }

    void awaitMigrationEntered() throws Exception {
      while (!migrationEntered.await(100, TimeUnit.MILLISECONDS)) {
        if (startup.isDone()) {
          startup.get();
          throw new IllegalStateException("Startup completed without entering the migration");
        }
      }
    }

    HttpResponse<String> healthcheck() throws Exception {
      HttpResponse<String> response =
          http.send(
              HttpRequest.newBuilder(
                      URI.create("http://127.0.0.1:" + port.get() + "/api/healthcheck"))
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      events.add("healthcheck " + response.statusCode());
      return response;
    }

    void releaseMigrationAndAwaitStartup() throws Exception {
      migrationReleased.countDown();
      startup.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
      migrationReleased.countDown();
      startup.handle((done, failure) -> null).join();
      context.get().close();
    }

    private Callback barrier() {
      return new Callback() {
        @Override
        public boolean supports(Event event, Context context) {
          return event == Event.BEFORE_MIGRATE || event == Event.AFTER_MIGRATE;
        }

        @Override
        public boolean canHandleInTransaction(Event event, Context context) {
          return false;
        }

        @Override
        public void handle(Event event, Context context) {
          if (event == Event.AFTER_MIGRATE) {
            events.add("migration completed");
            return;
          }
          events.add("migration entered");
          migrationEntered.countDown();
          try {
            migrationReleased.await();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
          }
        }

        @Override
        public String getCallbackName() {
          return "startup-migration-barrier";
        }
      };
    }
  }
}
