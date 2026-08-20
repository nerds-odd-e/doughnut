package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.entities.Grade;
import com.odde.doughnut.services.LearningSessionReportParser.ParseResult;
import com.odde.doughnut.services.LearningSessionReportParser.RejectedReportEntry;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
    assertRejected(result.rejected().get(0), "UnknownNote: 3", "Note title not found in notebook");
  }

  @Test
  void rejectsNonIntegerGrade() {
    ParseResult result = parser.parse("Hola: six\n", SPANISH_TITLES, Set.of());

    assertThat(result.entries(), empty());
    assertThat(result.rejected(), hasSize(1));
    assertRejected(result.rejected().get(0), "Hola: six", "Could not parse");
  }

  @Test
  void acceptsGradeFromOneToFour() {
    ParseResult result = parser.parse("Hola: 4\n", SPANISH_TITLES, Set.of());

    assertThat(result.rejected(), empty());
    assertThat(result.entries(), hasSize(1));
    assertEquals("Hola", result.entries().get(0).noteTitle());
    assertEquals(Grade.EASY, result.entries().get(0).grade());
  }

  @ParameterizedTest
  @CsvSource({"0", "5", "6"})
  void rejectsGradeOutsideOneToFour(int grade) {
    String line = "Hola: %d".formatted(grade);
    ParseResult result = parser.parse(line + "\n", SPANISH_TITLES, Set.of());

    assertThat(result.entries(), empty());
    assertThat(result.rejected(), hasSize(1));
    assertRejected(result.rejected().get(0), line, "Grade must be 1, 2, 3, or 4.");
  }

  @Test
  void rejectsDuplicateNotebookTitles() {
    ParseResult result = parser.parse("Hola: 4\n", SPANISH_TITLES, Set.of("Hola"));

    assertThat(result.entries(), empty());
    assertThat(result.rejected(), hasSize(1));
    assertRejected(result.rejected().get(0), "Hola: 4", "Ambiguous note title");
  }

  @Test
  void toleratesTrailingProseOnValidLine() {
    ParseResult result = parser.parse("Hola: 4 great session today\n", SPANISH_TITLES, Set.of());

    assertThat(result.rejected(), empty());
    assertThat(result.entries(), hasSize(1));
    assertEquals("Hola", result.entries().get(0).noteTitle());
    assertEquals(Grade.EASY, result.entries().get(0).grade());
  }

  @Test
  void rejectsDuplicateTitleInReport() {
    ParseResult result = parser.parse("Hola: 4\nHola: 3\n", SPANISH_TITLES, Set.of());

    assertThat(result.entries(), hasSize(1));
    assertEquals("Hola", result.entries().get(0).noteTitle());
    assertEquals(Grade.EASY, result.entries().get(0).grade());
    assertThat(result.rejected(), hasSize(1));
    assertRejected(result.rejected().get(0), "Hola: 3", "Duplicate note title");
  }

  @Test
  void skipsOptionalReportHeader() {
    ParseResult result =
        parser.parse(
            """
            # Learning Session Report

            Hola: 4
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
