package com.odde.doughnut.services;

import static com.odde.doughnut.services.LearningSessionReportParseAssertions.assertRejected;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.entities.Grade;
import com.odde.doughnut.services.LearningSessionReportParser.ParseResult;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LearningSessionReportFeedbackBlockParsingTest {

  private static final Set<String> SPANISH_TITLES = Set.of("Hola", "Gracias");

  private LearningSessionReportParser parser;

  @BeforeEach
  void setup() {
    parser = new LearningSessionReportParser();
  }

  @Test
  void parsesHeadingAndGrade() {
    ParseResult result =
        parser.parse(
            """
            # Learning Session Report

            <session_item_feedback>
            ### Hola
            Grade: 4
            Pronunciation was clear; still mixes ser/estar under pressure.

            ### Gracias
            Grade: 1
            Needed several reminders on the soft g.
            </session_item_feedback>
            """,
            SPANISH_TITLES,
            Set.of());

    assertThat(result.rejected(), empty());
    assertThat(result.entries(), hasSize(2));
    assertEquals("Hola", result.entries().get(0).noteTitle());
    assertEquals(Grade.EASY, result.entries().get(0).grade());
    assertEquals(
        "Pronunciation was clear; still mixes ser/estar under pressure.",
        result.entries().get(0).descriptiveText());
    assertEquals("Gracias", result.entries().get(1).noteTitle());
    assertEquals(Grade.AGAIN, result.entries().get(1).grade());
    assertEquals(
        "Needed several reminders on the soft g.", result.entries().get(1).descriptiveText());
  }

  @Test
  void gradeOnlyFeedbackItemLeavesDescriptiveTextNull() {
    ParseResult result =
        parser.parse(
            """
            <session_item_feedback>
            ### Hola
            Grade: 4
            </session_item_feedback>
            """,
            SPANISH_TITLES,
            Set.of());

    assertEquals(null, result.entries().get(0).descriptiveText());
  }

  @Test
  void blankProseLeavesDescriptiveTextNull() {
    ParseResult result =
        parser.parse(
            """
            <session_item_feedback>
            ### Hola
            Grade: 4

            </session_item_feedback>
            """,
            SPANISH_TITLES,
            Set.of());

    assertEquals(null, result.entries().get(0).descriptiveText());
  }

  @Test
  void rejectsItemWithoutGrade() {
    ParseResult result =
        parser.parse(
            """
            <session_item_feedback>
            ### Hola
            Pronunciation was clear.

            ### Gracias
            Grade: 1
            </session_item_feedback>
            """,
            SPANISH_TITLES,
            Set.of());

    assertThat(result.entries(), hasSize(1));
    assertEquals("Gracias", result.entries().get(0).noteTitle());
    assertThat(result.rejected(), hasSize(1));
    assertRejected(result.rejected().get(0), "### Hola", "Grade is required");
  }

  @Test
  void rejectsGradeOutsideOneToFour() {
    ParseResult result =
        parser.parse(
            """
            <session_item_feedback>
            ### Hola
            Grade: 5
            </session_item_feedback>
            """,
            SPANISH_TITLES,
            Set.of());

    assertThat(result.entries(), empty());
    assertThat(result.rejected(), hasSize(1));
    assertRejected(result.rejected().get(0), "Grade: 5", "Grade must be 1, 2, 3, or 4.");
  }

  @Test
  void rejectsUnknownTitle() {
    ParseResult result =
        parser.parse(
            """
            <session_item_feedback>
            ### UnknownNote
            Grade: 3
            </session_item_feedback>
            """,
            SPANISH_TITLES,
            Set.of());

    assertThat(result.entries(), empty());
    assertThat(result.rejected(), hasSize(1));
    assertRejected(result.rejected().get(0), "### UnknownNote", "Note title not found in notebook");
  }

  @Test
  void rejectsDuplicateTitle() {
    ParseResult result =
        parser.parse(
            """
            <session_item_feedback>
            ### Hola
            Grade: 4

            ### Hola
            Grade: 3
            </session_item_feedback>
            """,
            SPANISH_TITLES,
            Set.of());

    assertThat(result.entries(), hasSize(1));
    assertEquals("Hola", result.entries().get(0).noteTitle());
    assertEquals(Grade.EASY, result.entries().get(0).grade());
    assertThat(result.rejected(), hasSize(1));
    assertRejected(result.rejected().get(0), "### Hola", "Duplicate note title");
  }

  @Test
  void prefersFeedbackBlockOverLegacyGradesBlock() {
    ParseResult result =
        parser.parse(
            """
            # Learning Session Report

            <session_item_feedback>
            ### Hola
            Grade: 4
            ### Gracias
            Grade: 1
            </session_item_feedback>

            <session_item_grades>
            Hola: 1
            Gracias: 4
            </session_item_grades>
            """,
            SPANISH_TITLES,
            Set.of());

    assertThat(result.rejected(), empty());
    assertThat(result.entries(), hasSize(2));
    assertEquals("Hola", result.entries().get(0).noteTitle());
    assertEquals(Grade.EASY, result.entries().get(0).grade());
    assertEquals("Gracias", result.entries().get(1).noteTitle());
    assertEquals(Grade.AGAIN, result.entries().get(1).grade());
  }
}
