package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.ResolvedWikiLinkService;
import com.odde.donut.services.httpQuery.HttpClientAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NoteControllerShowTests extends ControllerTestBase {
  @Autowired NoteController controller;
  @Autowired ResolvedWikiLinkService resolvedWikiLinkService;
  @MockitoBean HttpClientAdapter httpClientAdapter;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
    Note note = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();
    assertThrows(UnexpectedNoAccessRightException.class, () -> controller.showNote(note));
  }

  @Test
  void shouldBeAbleToSeeOwnNote() throws UnexpectedNoAccessRightException {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    NoteRealm noteRealm = controller.showNote(note);
    assertThat(noteRealm.getId(), equalTo(note.getId()));
    assertThat(noteRealm.getNotebookRealm().readonly(), is(false));
    assertThat(
        noteRealm.getNotebookRealm().notebook().getId(), equalTo(note.getNotebook().getId()));
  }

  @Test
  void shouldReturnReadonlyWhenHavingReadingAuthOnly() throws UnexpectedNoAccessRightException {
    Note note = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();
    makeMe.aBazaarNotebook(note.getNotebook()).please();
    assertThat(controller.showNote(note).getNotebookRealm().readonly(), is(true));
  }

  @Test
  void rootRelativeNoteUrl_appearsAsInboundReferenceOnTarget()
      throws UnexpectedNoAccessRightException {
    Note target =
        makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("Url Target").please();
    Note source =
        makeMe
            .aNote()
            .underSameNotebookAs(target)
            .content("[any display](/n" + target.getId() + ")")
            .please();
    resolvedWikiLinkService.refreshForNote(source, currentUser.getUser());

    NoteRealm targetRealm = controller.showNote(target);
    assertThat(targetRealm.getReferences(), hasSize(1));
    assertThat(targetRealm.getReferences().getFirst().getId(), equalTo(source.getId()));
  }
}
