package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.focusContext.FocusContextResult;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NoteControllerGraphTests extends ControllerTestBase {
  @Autowired NoteController controller;
  @MockitoBean HttpClientAdapter httpClientAdapter;

  Note rootNote;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    rootNote = makeMe.aNote("Root").notebookOwnedBy(currentUser.getUser()).please();
  }

  @Test
  void shouldReturnGraphWithFocusNoteMetadata() throws UnexpectedNoAccessRightException {
    FocusContextResult result = controller.getGraph(rootNote, 5000);

    assertThat(result.getFocusNote().getNotebook(), equalTo(rootNote.getNotebook().getName()));
    assertThat(result.getFocusNote().getDepth(), equalTo(0));
    assertThat(result.getFocusNote().getOutgoingLinks(), is(notNullValue()));
  }

  @Test
  void shouldRespectCustomTokenLimit() throws UnexpectedNoAccessRightException {
    makeMe.aNote("Child").underSameNotebookAs(rootNote).please();
    FocusContextResult result = controller.getGraph(rootNote, 1);
    assertThat(result.getRelatedNotes(), is(empty()));
  }

  @Test
  void relatedNotesExposeEdgeTypeDepthAndPath() throws UnexpectedNoAccessRightException {
    makeMe.aNote("Linked").underSameNotebookAs(rootNote).content("See [[Root]]").please();

    FocusContextResult result = controller.getGraph(rootNote, 5000);
    assertThat(result.getRelatedNotes(), is(not(empty())));
    var related = result.getRelatedNotes().getFirst();
    assertThat(related.getEdgeType(), is(notNullValue()));
    assertThat(related.getDepth(), greaterThan(0));
    assertThat(related.getRetrievalPath(), is(notNullValue()));
  }

  @Test
  void shouldNotAllowAccessToUnauthorizedNotes() {
    Note unauthorizedNote = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();
    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller.getGraph(unauthorizedNote, 5000));
  }
}
