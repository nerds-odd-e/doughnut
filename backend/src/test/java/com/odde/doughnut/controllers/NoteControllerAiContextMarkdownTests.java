package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.NoteAiContextMarkdown;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NoteControllerAiContextMarkdownTests extends ControllerTestBase {
  @Autowired NoteController controller;
  @MockitoBean HttpClientAdapter httpClientAdapter;

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
}
