package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.services.LearningSessionReportParser.ParseResult;
import com.odde.doughnut.services.LearningSessionReportParser.RejectedReportEntry;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LearningSessionReportParserTest {

  private static final Set<String> SPANISH_TITLES = Set.of("Hola", "Gracias");

  private LearningSessionReportParser parser;

  @BeforeEach
  void setup() {
    parser = new LearningSessionReportParser();
  }

  @Test
  void rejectsUnknownTitle() {
    ParseResult result = parser.parse("UnknownNote: 3\n", SPANISH_TITLES, Set.of());

    assertThat(result.entries(), empty());
    assertThat(result.rejected(), hasSize(1));
    assertRejected(result.rejected().get(0), "UnknownNote: 3", "No session item matched");
  }

  @Test
  void rejectsNonIntegerScore() {
    ParseResult result = parser.parse("Hola: six\n", SPANISH_TITLES, Set.of());

    assertThat(result.entries(), empty());
    assertThat(result.rejected(), hasSize(1));
    assertRejected(result.rejected().get(0), "Hola: six", "Could not parse");
  }

  @Test
  void rejectsScoreOutsideZeroToFive() {
    ParseResult result = parser.parse("Hola: 6\n", SPANISH_TITLES, Set.of());

    assertThat(result.entries(), empty());
    assertThat(result.rejected(), hasSize(1));
    assertRejected(result.rejected().get(0), "Hola: 6", "Score must be between 0 and 5");
  }

  @Test
  void rejectsDuplicateNotebookTitles() {
    ParseResult result = parser.parse("Hola: 5\n", SPANISH_TITLES, Set.of("Hola"));

    assertThat(result.entries(), empty());
    assertThat(result.rejected(), hasSize(1));
    assertRejected(result.rejected().get(0), "Hola: 5", "Ambiguous note title");
  }

  @Test
  void toleratesTrailingProseOnValidLine() {
    ParseResult result = parser.parse("Hola: 5 great session today\n", SPANISH_TITLES, Set.of());

    assertThat(result.rejected(), empty());
    assertThat(result.entries(), hasSize(1));
    assertEquals("Hola", result.entries().get(0).noteTitle());
    assertEquals(5, result.entries().get(0).score());
  }

  @Test
  void skipsOptionalReportHeader() {
    ParseResult result =
        parser.parse(
            """
            # Learning Session Report

            Hola: 5
            Gracias: 1
            """,
            SPANISH_TITLES,
            Set.of());

    assertThat(result.rejected(), empty());
    assertThat(result.entries(), hasSize(2));
  }

  private void assertRejected(RejectedReportEntry rejected, String line, String reasonFragment) {
    assertEquals(line, rejected.line());
    org.junit.jupiter.api.Assertions.assertTrue(
        rejected.reason().contains(reasonFragment),
        () -> "expected reason containing '" + reasonFragment + "' but was: " + rejected.reason());
  }
}
