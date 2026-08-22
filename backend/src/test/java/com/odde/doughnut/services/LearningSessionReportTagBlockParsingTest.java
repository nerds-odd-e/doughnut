package com.odde.doughnut.services;

import static com.odde.doughnut.services.LearningSessionReportParseAssertions.assertRejected;
import static com.odde.doughnut.services.LearningSessionReportParser.SESSION_ITEM_GRADES_CLOSE_TAG;
import static com.odde.doughnut.services.LearningSessionReportParser.SESSION_ITEM_GRADES_OPEN_TAG;
import static com.odde.doughnut.services.LearningSessionReportParser.SESSION_ITEM_SCORES_CLOSE_TAG;
import static com.odde.doughnut.services.LearningSessionReportParser.SESSION_ITEM_SCORES_OPEN_TAG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.entities.Grade;
import com.odde.doughnut.services.LearningSessionReportParser.ParseResult;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LearningSessionReportTagBlockParsingTest {

  private static final Set<String> SPANISH_TITLES = Set.of("Hola", "Gracias");

  private LearningSessionReportParser parser;

  @BeforeEach
  void setup() {
    parser = new LearningSessionReportParser();
  }

  @ParameterizedTest
  @MethodSource("sessionItemTagPairs")
  void parsesGradesInsideTagIgnoringSurroundingProse(String openTag, String closeTag) {
    ParseResult result =
        parser.parse(
            """
            # Learning Session Report

            Thanks for a great session today.

            %s
            Hola: 4
            Gracias: 1
            %s

            Hola: 99
            """
                .formatted(openTag, closeTag),
            SPANISH_TITLES,
            Set.of());

    assertThat(result.rejected(), empty());
    assertThat(result.entries(), hasSize(2));
    assertEquals("Hola", result.entries().get(0).noteTitle());
    assertEquals(Grade.EASY, result.entries().get(0).grade());
    assertEquals("Gracias", result.entries().get(1).noteTitle());
    assertEquals(Grade.AGAIN, result.entries().get(1).grade());
  }

  @Test
  void returnsNoEntriesForEmptyTag() {
    ParseResult result =
        parser.parse(
            """
            # Learning Session Report

            %s
            %s
            """
                .formatted(SESSION_ITEM_GRADES_OPEN_TAG, SESSION_ITEM_GRADES_CLOSE_TAG),
            SPANISH_TITLES,
            Set.of());

    assertThat(result.entries(), empty());
    assertThat(result.rejected(), empty());
  }

  @Test
  void blankGradesTagFallsThroughToLegacyScores() {
    ParseResult result =
        parser.parse(
            """
            # Learning Session Report

            %s
            %s

            %s
            Hola: 4
            Gracias: 1
            %s
            """
                .formatted(
                    SESSION_ITEM_GRADES_OPEN_TAG,
                    SESSION_ITEM_GRADES_CLOSE_TAG,
                    SESSION_ITEM_SCORES_OPEN_TAG,
                    SESSION_ITEM_SCORES_CLOSE_TAG),
            SPANISH_TITLES,
            Set.of());

    assertThat(result.rejected(), empty());
    assertThat(result.entries(), hasSize(2));
    assertEquals("Hola", result.entries().get(0).noteTitle());
    assertEquals("Gracias", result.entries().get(1).noteTitle());
  }

  @Test
  void nonBlankUnparseableGradesBlockDoesNotFallThroughToScores() {
    ParseResult result =
        parser.parse(
            """
            # Learning Session Report

            %s
            not-a-grade-line
            %s

            %s
            Hola: 4
            %s
            """
                .formatted(
                    SESSION_ITEM_GRADES_OPEN_TAG,
                    SESSION_ITEM_GRADES_CLOSE_TAG,
                    SESSION_ITEM_SCORES_OPEN_TAG,
                    SESSION_ITEM_SCORES_CLOSE_TAG),
            SPANISH_TITLES,
            Set.of());

    assertThat(result.entries(), empty());
    assertThat(result.rejected(), hasSize(1));
    assertRejected(result.rejected().get(0), "not-a-grade-line", "Could not parse");
  }

  @Test
  void parsesGradesWhenClosingTagMissing() {
    ParseResult result =
        parser.parse(
            """
            %s
            Hola: 3
            """
                .formatted(SESSION_ITEM_GRADES_OPEN_TAG),
            SPANISH_TITLES,
            Set.of());

    assertThat(result.rejected(), empty());
    assertThat(result.entries(), hasSize(1));
    assertEquals("Hola", result.entries().get(0).noteTitle());
    assertEquals(Grade.GOOD, result.entries().get(0).grade());
  }

  private static Stream<Arguments> sessionItemTagPairs() {
    return Stream.of(
        Arguments.of(SESSION_ITEM_GRADES_OPEN_TAG, SESSION_ITEM_GRADES_CLOSE_TAG),
        Arguments.of(SESSION_ITEM_SCORES_OPEN_TAG, SESSION_ITEM_SCORES_CLOSE_TAG));
  }
}
