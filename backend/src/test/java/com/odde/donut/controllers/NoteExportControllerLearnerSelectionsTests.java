package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NoteExportControllerLearnerSelectionsTests extends ControllerTestBase {
  @Autowired NoteExportController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class LearnerLatestSelections {
    Note note;
    MemoryTracker exporterTracker;

    @BeforeEach
    void focusNote() {
      note = makeMe.aNote("Focus").content("Body").notebookOwnedBy(currentUser.getUser()).please();
      exporterTracker = makeMe.aMemoryTrackerFor(note).please();
    }

    @Test
    void threeQuestionsOneAnsweredShowsOwnSelectionSeparateFromDonutKey()
        throws UnexpectedNoAccessRightException {
      Mcq answered =
          makeMe
              .anMcq()
              .stem("answered-stem")
              .choices("picked-by-learner", "distractor", "donut-key-choice")
              .correctAnswerIndex(2)
              .please();
      makeMe
          .aRecallPrompt()
          .forMemoryTracker(exporterTracker)
          .withMcq(makeMe.anMcq().stem("unanswered-a").please())
          .please();
      makeMe
          .aRecallPrompt()
          .forMemoryTracker(exporterTracker)
          .withMcq(makeMe.anMcq().stem("unanswered-b").please())
          .please();
      makeMe
          .aRecallPrompt()
          .withMcq(answered)
          .forMemoryTracker(exporterTracker)
          .answerChoiceIndex(0)
          .please();

      String markdown =
          controller.getAiContextMarkdown(note, 5000, true, null, null, null).markdown();

      assertThat(
          markdown,
          stringContainsInOrder(
              "answered-stem",
              "Donut answer key: donut-key-choice",
              "Learner's selected answer: picked-by-learner"));
      assertThat(markdown, containsString("unanswered-a"));
      assertThat(markdown, containsString("unanswered-b"));
      assertThat(countOccurrences(markdown, "Learner's selected answer:"), is(1));
    }

    @Test
    void repeatedOwnAnswersKeepLatestByAnswerTime() throws UnexpectedNoAccessRightException {
      Mcq mcq =
          makeMe
              .anMcq()
              .stem("repeat-stem")
              .choices("older-pick", "newer-pick", "key-choice")
              .correctAnswerIndex(2)
              .please();
      makeMe
          .aRecallPrompt()
          .withMcq(mcq)
          .forMemoryTracker(exporterTracker)
          .answerChoiceIndex(0)
          .answerTimestamp(Timestamp.from(Instant.parse("2026-01-01T00:00:00Z")))
          .please();
      makeMe
          .aRecallPrompt()
          .withMcq(mcq)
          .forMemoryTracker(exporterTracker)
          .answerChoiceIndex(1)
          .answerTimestamp(Timestamp.from(Instant.parse("2026-06-01T00:00:00Z")))
          .please();

      String markdown =
          controller.getAiContextMarkdown(note, 5000, true, null, null, null).markdown();

      assertThat(markdown, containsString("Learner's selected answer: newer-pick"));
    }

    @Test
    void equalAnswerTimesPreferHigherAnswerId() throws UnexpectedNoAccessRightException {
      Timestamp sameTime = Timestamp.from(Instant.parse("2026-06-01T00:00:00Z"));
      Mcq mcq =
          makeMe
              .anMcq()
              .stem("tie-stem")
              .choices("lower-id-pick", "higher-id-pick", "key-choice")
              .correctAnswerIndex(2)
              .please();
      makeMe
          .aRecallPrompt()
          .withMcq(mcq)
          .forMemoryTracker(exporterTracker)
          .answerChoiceIndex(0)
          .answerTimestamp(sameTime)
          .please();
      makeMe
          .aRecallPrompt()
          .withMcq(mcq)
          .forMemoryTracker(exporterTracker)
          .answerChoiceIndex(1)
          .answerTimestamp(sameTime)
          .please();

      String markdown =
          controller.getAiContextMarkdown(note, 5000, true, null, null, null).markdown();

      assertThat(markdown, containsString("Learner's selected answer: higher-id-pick"));
    }

    private static int countOccurrences(String haystack, String needle) {
      int count = 0;
      for (int from = 0; (from = haystack.indexOf(needle, from)) >= 0; from += needle.length()) {
        count++;
      }
      return count;
    }
  }
}
