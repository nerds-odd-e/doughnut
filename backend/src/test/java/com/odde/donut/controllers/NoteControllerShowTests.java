package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.httpQuery.HttpClientAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NoteControllerShowTests extends ControllerTestBase {
  @Autowired NoteController controller;
  @Autowired TextContentController textContentController;
  @MockitoBean HttpClientAdapter httpClientAdapter;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  private void authorWikiLinkTo(Note carrier, String targetTitle)
      throws UnexpectedNoAccessRightException {
    NoteUpdateContentDTO contentDto = new NoteUpdateContentDTO();
    contentDto.setContent("See [[" + targetTitle + "]].");
    textContentController.updateNoteContent(carrier, contentDto);
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
    Note source = makeMe.aNote().underSameNotebookAs(target).please();
    NoteUpdateContentDTO contentDto = new NoteUpdateContentDTO();
    contentDto.setContent("[any display](/n" + target.getId() + ")");
    textContentController.updateNoteContent(source, contentDto);

    NoteRealm targetRealm = controller.showNote(target);
    assertThat(targetRealm.getReferences(), hasSize(1));
    assertThat(targetRealm.getReferences().getFirst().getId(), equalTo(source.getId()));
  }

  @Test
  void newlyCreatedTargetImmediatelyGainsCurrentInboundReferencesWithoutRefreshingSource()
      throws UnexpectedNoAccessRightException {
    Note source = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    authorWikiLinkTo(source, "Future");

    // "Future" does not exist yet: the authored wiki link is missing, so it resolves to nothing.
    assertThat(controller.showNote(source).getWikiLinks(), hasSize(0));

    Note future = makeMe.aNote().underSameNotebookAs(source).title("Future").please();

    // Showing "Future" — without any refresh/re-save of "source" — already lists it as inbound.
    NoteRealm futureRealm = controller.showNote(future);
    assertThat(futureRealm.getReferences(), hasSize(1));
    assertThat(futureRealm.getReferences().getFirst().getId(), equalTo(source.getId()));
  }

  @Test
  void namesakeCreationRemovesNowAmbiguousInboundReferenceFromBothTargets()
      throws UnexpectedNoAccessRightException {
    Folder homeFolder = makeMe.aFolder().notebookOwnedBy(currentUser.getUser()).please();
    Note target = makeMe.aNote().title("Target").folder(homeFolder).please();
    Note source = makeMe.aNote().underSameNotebookAs(target).please();
    authorWikiLinkTo(source, "Target");

    // Unique "Target" resolves: source appears inbound.
    assertThat(controller.showNote(target).getReferences(), hasSize(1));
    assertThat(
        controller.showNote(target).getReferences().getFirst().getId(), equalTo(source.getId()));

    // A namesake in a different folder of the same notebook makes "[[Target]]" ambiguous between
    // the two same-titled notes (a plain, non-path-shaped token doesn't pin a folder).
    Folder otherFolder = makeMe.aFolder().notebook(homeFolder.getNotebook()).please();
    Note namesake = makeMe.aNote().title("Target").folder(otherFolder).please();

    // Neither same-named note lists source as inbound any more.
    assertThat(controller.showNote(target).getReferences(), hasSize(0));
    assertThat(controller.showNote(namesake).getReferences(), hasSize(0));

    // The source's own authored content/entry is unchanged (still the same authored token).
    NoteRealm sourceRealm = controller.showNote(source);
    assertThat(sourceRealm.getId(), equalTo(source.getId()));
  }
}
