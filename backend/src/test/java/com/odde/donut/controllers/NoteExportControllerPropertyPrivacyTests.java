package com.odde.donut.controllers;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class NoteExportControllerPropertyPrivacyTests extends ControllerTestBase {
  @Autowired MockMvc mockMvc;
  Note note;
  MemoryTracker tracker;
  Mcq question;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    note =
        makeMe
            .aNote("Focus")
            .notebookOwnedBy(currentUser.getUser())
            .content("---\ntopic: Biology\n---\nBody")
            .please();
    tracker = makeMe.aMemoryTrackerFor(note).propertyKey("topic").please();
    question =
        makeMe
            .anMcq()
            .forNote(note)
            .stem("focused-question")
            .choices("first-pick", "latest-pick", "key")
            .correctAnswerIndex(2)
            .please();
    makeMe
        .aRecallPrompt()
        .withMcq(question)
        .forMemoryTracker(tracker)
        .answerChoiceIndex(0)
        .answerTimestamp(Timestamp.from(Instant.parse("2026-01-01T00:00:00Z")))
        .please();
  }

  @Test
  void focusedRequestUsesLatestOwnAnswerAndKeepsCompleteAppendixOutsideBudget() throws Exception {
    makeMe
        .aRecallPrompt()
        .withMcq(question)
        .forMemoryTracker(tracker)
        .answerChoiceIndex(1)
        .answerTimestamp(Timestamp.from(Instant.parse("2026-02-01T00:00:00Z")))
        .please();
    MemoryTracker other =
        makeMe.aMemoryTrackerFor(note).by(makeMe.aUser().please()).propertyKey("topic").please();
    makeMe
        .aRecallPrompt()
        .withMcq(
            makeMe
                .anMcq()
                .forNote(note)
                .stem("another-learner-question")
                .choices("other-pick", "unused", "key")
                .please())
        .forMemoryTracker(other)
        .answerChoiceIndex(0)
        .answerTimestamp(Timestamp.from(Instant.parse("2026-10-01T00:00:00Z")))
        .please();
    mockMvc
        .perform(request(0))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.markdown", containsString("Property value: Biology")))
        .andExpect(jsonPath("$.markdown", containsString("   - key")))
        .andExpect(jsonPath("$.markdown", containsString("Donut answer key: key")))
        .andExpect(jsonPath("$.markdown", containsString("Learner's selected answer: latest-pick")))
        .andExpect(
            jsonPath("$.markdown", not(containsString("Learner's selected answer: other-pick"))));
  }

  @Test
  void focusedGuestReceivesSharedQuestionsWithoutPersonalAnswers() throws Exception {
    makeMe.aBazaarNotebook(note.getNotebook()).please();
    currentUser.setUser(null);
    mockMvc
        .perform(request())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.markdown", containsString("focused-question")))
        .andExpect(jsonPath("$.markdown", not(containsString("Learner's selected answer:"))));
  }

  @Test
  void focusedRequestCannotReadAnUnauthorizedNote() throws Exception {
    currentUser.setUser(makeMe.aUser().please());
    mockMvc.perform(request()).andExpect(status().isForbidden());
  }

  private MockHttpServletRequestBuilder request() {
    return request(5000);
  }

  private MockHttpServletRequestBuilder request(int tokenLimit) {
    return get("/api/notes/{note}/ai-context-markdown", note.getId())
        .param("tokenLimit", Integer.toString(tokenLimit))
        .param("includeRecentQuestions", "true")
        .param("propertyKey", "topic");
  }
}
