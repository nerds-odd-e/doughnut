package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.odde.donut.controllers.dto.NoteAiContextMarkdown;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

class NoteExportControllerAiContextMarkdownTests extends ControllerTestBase {
  @Autowired NoteExportController controller;
  @Autowired MockMvc mockMvc;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void shouldReturnMarkdownForReadableNote() throws UnexpectedNoAccessRightException {
    Note note =
        makeMe.aNote("Focus").content("Body").notebookOwnedBy(currentUser.getUser()).please();
    NoteAiContextMarkdown dto = controller.getAiContextMarkdown(note, 5000, false);
    assertThat(dto.markdown(), containsString("Focus"));
    assertThat(dto.markdown(), containsString("Body"));
  }

  @Test
  void shouldNotAllowAccessToUnauthorizedNotes() {
    Note unauthorizedNote = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.getAiContextMarkdown(unauthorizedNote, 5000, false));
  }

  @Test
  void realHttpGetReturnsMarkdownWithTokenLimitQuery() throws Exception {
    Note note =
        makeMe.aNote("Focus").content("Body").notebookOwnedBy(currentUser.getUser()).please();

    mockMvc
        .perform(
            get("/api/notes/{note}/ai-context-markdown", note.getId())
                .param("tokenLimit", "5000")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.markdown", containsString("Focus")))
        .andExpect(jsonPath("$.markdown", containsString("Body")));
  }

  @Test
  void bazaarGuestGetsOrdinaryExportViaHttp() throws Exception {
    Note note =
        makeMe
            .aNote("Shared Focus")
            .content("Shared body")
            .notebookOwnedBy(makeMe.aUser().please())
            .please();
    makeMe.aBazaarNotebook(note.getNotebook()).please();
    currentUser.setUser(null);

    mockMvc
        .perform(
            get("/api/notes/{note}/ai-context-markdown", note.getId())
                .param("tokenLimit", "2000")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.markdown", containsString("Shared Focus")))
        .andExpect(jsonPath("$.markdown", containsString("Shared body")));
  }

  @Nested
  class RecentQuestionsAppendix {
    Note note;

    @BeforeEach
    void focusNote() {
      note = makeMe.aNote("Focus").content("Body").notebookOwnedBy(currentUser.getUser()).please();
    }

    @Test
    void includeTrueRendersCanonicalAppendixShape() throws UnexpectedNoAccessRightException {
      makeMe
          .anMcq()
          .forNote(note)
          .stem("What is two plus two?")
          .choices("three", "four", "five")
          .correctAnswerIndex(1)
          .please();

      String markdown = controller.getAiContextMarkdown(note, 5000, true).markdown();

      assertThat(markdown, containsString("Focus"));
      assertThat(markdown, containsString("Body"));
      assertThat(markdown, containsString("## Recent multiple-choice questions"));
      assertThat(markdown, containsString("1. What is two plus two?"));
      assertThat(markdown, containsString("   - three"));
      assertThat(markdown, containsString("   - four"));
      assertThat(markdown, containsString("   - five"));
      assertThat(markdown, containsString("   Donut answer key: four"));
    }

    @Test
    void newestTenByCreationThenIdIgnoresInsertionOrderAndIncludesEqualTimestampTies()
        throws UnexpectedNoAccessRightException {
      Timestamp older = Timestamp.from(Instant.parse("2026-01-01T00:00:00Z"));
      Timestamp mid = Timestamp.from(Instant.parse("2026-06-01T00:00:00Z"));
      Timestamp newest = Timestamp.from(Instant.parse("2026-11-01T00:00:00Z"));
      Timestamp tie = Timestamp.from(Instant.parse("2026-08-01T00:00:00Z"));

      // Insertion order deliberately differs from creation chronology.
      makeMe.anMcq().forNote(note).stem("oldest-kept-out").createdAt(older).please();
      makeMe.anMcq().forNote(note).stem("mid-01").createdAt(mid).please();
      makeMe.anMcq().forNote(note).stem("mid-02").createdAt(mid).please();
      makeMe.anMcq().forNote(note).stem("mid-03").createdAt(mid).please();
      makeMe.anMcq().forNote(note).stem("mid-04").createdAt(mid).please();
      makeMe.anMcq().forNote(note).stem("mid-05").createdAt(mid).please();
      makeMe.anMcq().forNote(note).stem("mid-06").createdAt(mid).please();
      makeMe.anMcq().forNote(note).stem("tie-lower-id").createdAt(tie).please();
      makeMe.anMcq().forNote(note).stem("tie-higher-id").createdAt(tie).please();
      makeMe.anMcq().forNote(note).stem("second-oldest-kept-out").createdAt(older).please();
      makeMe.anMcq().forNote(note).stem("newest-first").createdAt(newest).please();
      makeMe.anMcq().forNote(note).stem("mid-07").createdAt(mid).please();

      String markdown = controller.getAiContextMarkdown(note, 5000, true).markdown();
      String appendix = markdown.substring(markdown.indexOf("## Recent multiple-choice questions"));

      assertThat(
          appendix,
          stringContainsInOrder(
              "1. newest-first",
              "2. tie-higher-id",
              "3. tie-lower-id",
              "4. mid-07",
              "5. mid-06",
              "6. mid-05",
              "7. mid-04",
              "8. mid-03",
              "9. mid-02",
              "10. mid-01"));
      assertThat(appendix, not(containsString("oldest-kept-out")));
      assertThat(appendix, not(containsString("second-oldest-kept-out")));
    }

    @Test
    void anotherNotesQuestionsAreExcluded() throws UnexpectedNoAccessRightException {
      makeMe.anMcq().forNote(note).stem("focus-stem").please();
      Note other = makeMe.aNote("Other").notebookOwnedBy(currentUser.getUser()).please();
      makeMe.anMcq().forNote(other).stem("other-stem").please();

      String markdown = controller.getAiContextMarkdown(note, 5000, true).markdown();

      assertThat(markdown, containsString("focus-stem"));
      assertThat(markdown, not(containsString("other-stem")));
    }

    @Test
    void questionsWithoutPromptsAreIncluded() throws UnexpectedNoAccessRightException {
      makeMe.anMcq().forNote(note).stem("no-prompt-stem").please();

      String markdown = controller.getAiContextMarkdown(note, 5000, true).markdown();

      assertThat(markdown, containsString("no-prompt-stem"));
    }

    @Test
    void contestedQuestionsAreIncludedWithoutSchedulingFilter()
        throws UnexpectedNoAccessRightException {
      makeMe.anMcq().forNote(note).stem("contested-stem").contested().please();

      String markdown = controller.getAiContextMarkdown(note, 5000, true).markdown();

      assertThat(markdown, containsString("contested-stem"));
    }

    @Test
    void zeroQuestionsStillExportsOrdinaryContext() throws UnexpectedNoAccessRightException {
      String markdown = controller.getAiContextMarkdown(note, 5000, true).markdown();

      assertThat(markdown, containsString("Focus"));
      assertThat(markdown, containsString("Body"));
      assertThat(markdown, not(containsString("## Recent multiple-choice questions")));
    }

    @Test
    void omittedIncludeRecentQuestionsDefaultsToOrdinaryContextViaHttp() throws Exception {
      makeMe.anMcq().forNote(note).stem("hidden-by-default").please();

      performExportHttp(/* includeRecentQuestions */ null)
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.markdown", containsString("Focus")))
          .andExpect(jsonPath("$.markdown", not(containsString("hidden-by-default"))))
          .andExpect(
              jsonPath("$.markdown", not(containsString("## Recent multiple-choice questions"))));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void includeRecentQuestionsHttpBinding(boolean includeRecentQuestions) throws Exception {
      makeMe.anMcq().forNote(note).stem("binding-stem").please();

      ResultActions result =
          performExportHttp(Boolean.toString(includeRecentQuestions)).andExpect(status().isOk());

      if (includeRecentQuestions) {
        result.andExpect(jsonPath("$.markdown", containsString("binding-stem")));
      } else {
        result.andExpect(jsonPath("$.markdown", not(containsString("binding-stem"))));
      }
    }

    private ResultActions performExportHttp(String includeRecentQuestions) throws Exception {
      var request =
          get("/api/notes/{note}/ai-context-markdown", note.getId())
              .param("tokenLimit", "5000")
              .accept(MediaType.APPLICATION_JSON);
      if (includeRecentQuestions != null) {
        request.param("includeRecentQuestions", includeRecentQuestions);
      }
      return mockMvc.perform(request);
    }
  }
}
