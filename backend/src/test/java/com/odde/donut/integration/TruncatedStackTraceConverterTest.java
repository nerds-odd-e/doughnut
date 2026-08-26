package com.odde.donut.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

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
    logger = loggerContext.getLogger("com.odde.donut");
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

    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(loggerContext);
    encoder.setPattern("%msg%n%truncatedEx");
    encoder.start();

    String formattedMessage = new String(encoder.encode(listAppender.list.getLast()));
    String[] lines = formattedMessage.split("\n");

    assertThat(formattedMessage, containsString("Test exception"));
    assertThat(lines.length, lessThanOrEqualTo(25));
    assertThat(formattedMessage, containsString("lines of spring internal calls"));
    assertThat(formattedMessage, containsString("com.odde.donut"));
  }
}
