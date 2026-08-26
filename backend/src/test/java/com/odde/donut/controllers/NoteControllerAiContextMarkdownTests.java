package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NoteAiContextMarkdown;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.httpQuery.HttpClientAdapter;
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
