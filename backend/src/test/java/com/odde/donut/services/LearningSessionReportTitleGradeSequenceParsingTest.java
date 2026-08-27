package com.odde.donut.services;

import static com.odde.donut.services.LearningSessionReportParseAssertions.assertRejected;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.donut.services.LearningSessionReportParser.ParseResult;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LearningSessionReportTitleGradeSequenceParsingTest {

  private static final Set<String> SPANISH_TITLES = Set.of("Hola", "Gracias");

  private LearningSessionReportParser parser;

  @BeforeEach
  void setup() {
    parser = new LearningSessionReportParser();
  }

  @Test
  void parsesTitleGradeSequenceWhenFeedbackHasNoSessionItemTags() {
    ParseResult result =
        parser.parse(
            """
            # Learning Session Report

            <session_item_feedback>
            Hola: 4
            Pronunciation was clear; still mixes ser/estar under pressure.
            Gracias: 1
            Needed several reminders on the soft g.
            </session_item_feedback>
            """,
            SPANISH_TITLES,
            Set.of());

    assertThat(result.rejected(), empty());
    assertThat(result.entries(), hasSize(2));
    assertEquals("Hola", result.entries().get(0).noteTitle());
    assertEquals(
        "Pronunciation was clear; still mixes ser/estar under pressure.",
        result.entries().get(0).descriptiveText());
    assertEquals("Gracias", result.entries().get(1).noteTitle());
    assertEquals(
        "Needed several reminders on the soft g.", result.entries().get(1).descriptiveText());
  }

  @Test
  void rejectsOutOfRangeGradeInTitleGradeSequenceAndRecordsFollowingItem() {
    ParseResult result =
        parser.parse(
            """
            <session_item_feedback>
            Hola: 5
            Gracias: 1
            Needed several reminders on the soft g.
            </session_item_feedback>
            """,
            SPANISH_TITLES,
            Set.of());

    assertThat(result.rejected(), hasSize(1));
    assertRejected(result.rejected().get(0), "Hola: 5", "Grade must be 1, 2, 3, or 4.");
    assertThat(result.entries(), hasSize(1));
    assertEquals("Gracias", result.entries().get(0).noteTitle());
  }

  @Test
  void nonTitleGradeLineStaysInPreviousItemText() {
    ParseResult result =
        parser.parse(
            """
            <session_item_feedback>
            Hola: 4
            Pronunciation was clear.
            Something: 1
            Gracias: 1
            Needed several reminders on the soft g.
            </session_item_feedback>
            """,
            SPANISH_TITLES,
            Set.of());

    assertThat(result.rejected(), empty());
    assertThat(result.entries(), hasSize(2));
    assertEquals(
        "Pronunciation was clear.\nSomething: 1", result.entries().get(0).descriptiveText());
    assertEquals(
        "Needed several reminders on the soft g.", result.entries().get(1).descriptiveText());
  }
}
