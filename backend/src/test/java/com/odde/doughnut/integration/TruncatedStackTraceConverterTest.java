package com.odde.doughnut.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TruncatedStackTraceConverterTest {
  private ListAppender<ILoggingEvent> listAppender;
  private Logger logger;
  private Level originalLevel;

  @BeforeEach
  void setup() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    logger = loggerContext.getLogger("com.odde.doughnut");
    originalLevel = logger.getLevel();
    logger.setLevel(Level.ERROR);
    listAppender = new ListAppender<>();
    listAppender.setContext(loggerContext);
    listAppender.start();
    logger.addAppender(listAppender);
  }

  @AfterEach
  void cleanup() {
    if (listAppender != null) {
      listAppender.stop();
      logger.detachAppender(listAppender);
    }
    if (logger != null && originalLevel != null) {
      logger.setLevel(originalLevel);
    }
  }

  @Test
  void shouldTruncateAndFilterStacktraceInLogs() {
    try {
      throw new RuntimeException("Test exception for stacktrace truncation");
    } catch (RuntimeException e) {
      logger.error("Test error message", e);
    }

    assertThat("Should have logged at least one event", listAppender.list.size(), greaterThan(0));

    ILoggingEvent lastEvent = listAppender.list.get(listAppender.list.size() - 1);

    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(loggerContext);
    encoder.setPattern("%msg%n%truncatedEx");
    encoder.start();

    String formattedMessage = new String(encoder.encode(lastEvent));

    assertThat("Log should contain exception", formattedMessage, notNullValue());
    assertThat("Log should contain stacktrace", formattedMessage, containsString("Test exception"));

    String[] lines = formattedMessage.split("\n");

    assertThat(
        "Stacktrace should be truncated to 20 lines or less", lines.length, lessThanOrEqualTo(25));

    boolean hasSpringInternalCollapse = false;
    boolean hasApplicationCode = false;
    for (String line : lines) {
      if (line.contains("...") && line.contains("lines of spring internal calls")) {
        hasSpringInternalCollapse = true;
      }
      if (line.contains("com.odde.doughnut")) {
        hasApplicationCode = true;
      }
    }

    assertThat("Should collapse Spring internal calls", hasSpringInternalCollapse, equalTo(true));
    assertThat("Should include application code", hasApplicationCode, equalTo(true));

    boolean hasSpringInternalDirect = false;
    for (String line : lines) {
      if (line.contains("org.springframework.") && !line.contains("...")) {
        hasSpringInternalDirect = true;
        break;
      }
    }

    assertThat(
        "Should not include direct Spring internal stack frames",
        hasSpringInternalDirect,
        equalTo(false));
  }
}
