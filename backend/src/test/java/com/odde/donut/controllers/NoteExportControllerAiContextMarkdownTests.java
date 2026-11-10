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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
    NoteAiContextMarkdown dto = controller.getAiContextMarkdown(note, 5000);
    assertThat(dto.markdown(), containsString("Focus"));
    assertThat(dto.markdown(), containsString("Body"));
  }

  @Test
  void shouldNotAllowAccessToUnauthorizedNotes() {
    Note unauthorizedNote = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.getAiContextMarkdown(unauthorizedNote, 5000));
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
}
