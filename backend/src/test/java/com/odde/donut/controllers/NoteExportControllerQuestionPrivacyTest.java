package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class NoteExportControllerQuestionPrivacyTest extends ControllerTestBase {
  @Autowired NoteExportController controller;
  @Autowired MockMvc mockMvc;
  Note note;
  MemoryTracker exporterTracker;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    note = makeMe.aNote("Focus").content("Body").notebookOwnedBy(currentUser.getUser()).please();
    exporterTracker = makeMe.aMemoryTrackerFor(note).please();
  }

  @Test
  void newerOtherLearnerAttemptDoesNotSubstitute() throws UnexpectedNoAccessRightException {
    Mcq mcq =
        makeMe
            .anMcq()
            .forNote(note)
            .stem("exporter-stem")
            .choices("exporter-pick", "other-pick", "key-choice")
            .correctAnswerIndex(2)
            .please();
    makeMe
        .aRecallPrompt()
        .withMcq(mcq)
        .forMemoryTracker(exporterTracker)
        .answerChoiceIndex(0)
        .answerTimestamp(Timestamp.from(Instant.parse("2026-01-01T00:00:00Z")))
        .please();
    User other = makeMe.aUser().please();
    makeMe
        .aRecallPrompt()
        .withMcq(
            makeMe
                .anMcq()
                .forNote(note)
                .stem("other-learner-stem")
                .choices("exporter-pick", "other-pick", "key-choice")
                .correctAnswerIndex(2)
                .please())
        .forMemoryTracker(makeMe.aMemoryTrackerFor(note).by(other).please())
        .answerChoiceIndex(1)
        .answerTimestamp(Timestamp.from(Instant.parse("2026-11-01T00:00:00Z")))
        .please();

    String markdown =
        controller.getAiContextMarkdown(note, 5000, true, null, null, null).markdown();

    assertThat(markdown, containsString("Learner's selected answer: exporter-pick"));
    assertThat(markdown, not(containsString("Learner's selected answer: other-pick")));
  }

  @Test
  void questionAnsweredOnlyByOtherLearnerHasNoSelectionForExporter()
      throws UnexpectedNoAccessRightException {
    Mcq mcq =
        makeMe
            .anMcq()
            .forNote(note)
            .stem("other-only-stem")
            .choices("other-pick", "unused", "key-choice")
            .correctAnswerIndex(2)
            .please();
    User other = makeMe.aUser().please();
    makeMe
        .aRecallPrompt()
        .withMcq(mcq)
        .forMemoryTracker(makeMe.aMemoryTrackerFor(note).by(other).please())
        .answerChoiceIndex(0)
        .please();

    String markdown =
        controller.getAiContextMarkdown(note, 5000, true, null, null, null).markdown();

    assertThat(markdown, containsString("other-only-stem"));
    assertThat(markdown, containsString("Donut answer key: key-choice"));
    assertThat(markdown, not(containsString("Learner's selected answer:")));
  }

  @Test
  void guestIncludeRecentQuestionsGetsKeysWithoutPersonalSelections() throws Exception {
    Mcq mcq =
        makeMe
            .anMcq()
            .forNote(note)
            .stem("guest-stem")
            .choices("owner-pick", "unused", "key-choice")
            .correctAnswerIndex(2)
            .please();
    makeMe
        .aRecallPrompt()
        .withMcq(mcq)
        .forMemoryTracker(exporterTracker)
        .answerChoiceIndex(0)
        .please();
    makeMe.aBazaarNotebook(note.getNotebook()).please();
    currentUser.setUser(null);

    mockMvc
        .perform(
            get("/api/notes/{note}/ai-context-markdown", note.getId())
                .param("tokenLimit", "5000")
                .param("includeRecentQuestions", "true")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.markdown", containsString("guest-stem")))
        .andExpect(jsonPath("$.markdown", containsString("Donut answer key: key-choice")))
        .andExpect(jsonPath("$.markdown", not(containsString("Learner's selected answer:"))));
  }

  @Test
  void unreadableNoteStillRefusedWhenIncludingRecentQuestions() {
    Note unauthorized = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.getAiContextMarkdown(unauthorized, 5000, true, null, null, null));
  }
}
