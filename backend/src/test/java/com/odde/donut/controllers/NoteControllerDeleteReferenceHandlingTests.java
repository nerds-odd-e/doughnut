package com.odde.donut.controllers;

import static com.odde.donut.entities.repositories.AuthoredNoteReferenceRowTestSupport.rowsFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.controllers.dto.NoteDeleteDTO;
import com.odde.donut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.AuthoredNoteReferenceRow;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.httpQuery.HttpClientAdapter;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Note-delete reference-handling policies (REMOVE_FROM_PROPERTIES / LEAVE_DEAD_LINKS). */
class NoteControllerDeleteReferenceHandlingTests extends ControllerTestBase {
  @Autowired NoteController controller;
  @Autowired TextContentController textContentController;
  @Autowired EntityManager entityManager;
  @MockitoBean HttpClientAdapter httpClientAdapter;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  private NoteDeleteDTO leaveDeadLinksDeleteRequest() {
    NoteDeleteDTO dto = new NoteDeleteDTO();
    dto.setReferenceHandling(NoteDeleteReferenceHandling.LEAVE_DEAD_LINKS);
    return dto;
  }

  private NoteDeleteDTO removeFromPropertiesDeleteRequest() {
    NoteDeleteDTO dto = new NoteDeleteDTO();
    dto.setReferenceHandling(NoteDeleteReferenceHandling.REMOVE_FROM_PROPERTIES);
    return dto;
  }

  @Test
  void shouldRemoveDeletedNoteLinksFromReferrerPropertiesOnly()
      throws UnexpectedNoAccessRightException {
    Note target = makeMe.aNote("Target").notebookOwnedBy(currentUser.getUser()).please();
    Note unrelated = makeMe.aNote("Unrelated").underSameNotebookAs(target).please();
    Note referrer = makeMe.aNote("Referrer").underSameNotebookAs(target).please();
    NoteUpdateContentDTO content = new NoteUpdateContentDTO();
    content.setContent("---\ntarget: \"[[Target]]\"\n---\nBody [[Unrelated]]");
    textContentController.updateNoteContent(referrer, content);

    controller.deleteNote(target, removeFromPropertiesDeleteRequest());

    assertThat(referrer.getContent(), equalTo("---\ntype: Note\n---\nBody [[Unrelated]]"));
    assertThat(
        rowsFor(entityManager, referrer).stream()
            .map(AuthoredNoteReferenceRow::getAuthoredLink)
            .toList(),
        contains("Unrelated"));
    assertThat(
        controller.showNote(referrer).getWikiLinks().stream()
            .map(WikiLink::getDestinationNoteId)
            .toList(),
        contains(unrelated.getId()));
  }

  @Test
  void shouldRemovePropertyReferenceAuthoredBeforeTheTargetNoteExisted()
      throws UnexpectedNoAccessRightException {
    Note referrer = makeMe.aNote("Referrer").notebookOwnedBy(currentUser.getUser()).please();
    NoteUpdateContentDTO content = new NoteUpdateContentDTO();
    content.setContent("---\ntarget: \"[[Future]]\"\n---\nBody");
    textContentController.updateNoteContent(referrer, content);
    Note target = makeMe.aNote("Future").underSameNotebookAs(referrer).please();

    controller.deleteNote(target, removeFromPropertiesDeleteRequest());

    assertThat(referrer.getContent(), equalTo("---\ntype: Note\n---\nBody"));
  }

  @Test
  void shouldNotTouchUnrelatedNotesAuthoredReferenceRowsOnDelete()
      throws UnexpectedNoAccessRightException {
    Note target = makeMe.aNote("Target").notebookOwnedBy(currentUser.getUser()).please();
    makeMe.aNote("Elsewhere").underSameNotebookAs(target).please();
    Note unrelatedReferrer = makeMe.aNote("UnrelatedReferrer").underSameNotebookAs(target).please();
    NoteUpdateContentDTO content = new NoteUpdateContentDTO();
    content.setContent("Body [[Elsewhere]]");
    textContentController.updateNoteContent(unrelatedReferrer, content);
    List<AuthoredNoteReferenceRow> rowsBeforeDelete = rowsFor(entityManager, unrelatedReferrer);

    controller.deleteNote(target, leaveDeadLinksDeleteRequest());

    assertThat(rowsFor(entityManager, unrelatedReferrer), equalTo(rowsBeforeDelete));
    assertThat(
        rowsFor(entityManager, unrelatedReferrer).stream()
            .map(AuthoredNoteReferenceRow::getAuthoredLink)
            .toList(),
        contains("Elsewhere"));
  }

  @Test
  void shouldNotRemovePropertyReferenceThatHasBecomeAmbiguousSinceItWasCached()
      throws UnexpectedNoAccessRightException {
    Note target = makeMe.aNote("Target").notebookOwnedBy(currentUser.getUser()).please();
    Note referrer = makeMe.aNote("Referrer").underSameNotebookAs(target).please();
    NoteUpdateContentDTO content = new NoteUpdateContentDTO();
    content.setContent("---\ntarget: \"[[Target]]\"\n---\nBody");
    textContentController.updateNoteContent(referrer, content);
    // Introduce a same-titled namesake in a different folder after the referrer's row was cached,
    // without touching the referrer's own content — its cached resolved row still says "Target",
    // but the token now live-resolves ambiguously.
    Folder otherFolder =
        makeMe.aFolder().notebook(target.getNotebook()).name("Other Folder").please();
    makeMe.aNote("Target").folder(otherFolder).please();

    controller.deleteNote(target, removeFromPropertiesDeleteRequest());

    assertThat(referrer.getContent(), containsString("[[Target]]"));
  }

  @Test
  void shouldReresolveNotebookShorthandsWhenDeleteRemovesACollision()
      throws UnexpectedNoAccessRightException {
    Note target1 = makeMe.aNote("Target").notebookOwnedBy(currentUser.getUser()).please();
    Folder otherFolder =
        makeMe.aFolder().notebook(target1.getNotebook()).name("Other Folder").please();
    Note target2 = makeMe.aNote("Target").folder(otherFolder).please();
    Note referrer =
        makeMe.aNote("Referrer").underSameNotebookAs(target1).content("See [[Target]].").please();
    assertThat(
        controller.showNote(referrer).getWikiLinks().get(0).getResolution(),
        equalTo(WikiLink.Resolution.AMBIGUOUS));

    controller.deleteNote(target2, leaveDeadLinksDeleteRequest());

    assertThat(
        controller.showNote(referrer).getWikiLinks().get(0).getResolution(),
        equalTo(WikiLink.Resolution.RESOLVED));
  }

  @Test
  void shouldReresolveNotebookShorthandsWhenRestoreReintroducesACollision()
      throws UnexpectedNoAccessRightException {
    Note target1 = makeMe.aNote("Target").notebookOwnedBy(currentUser.getUser()).please();
    Folder otherFolder =
        makeMe.aFolder().notebook(target1.getNotebook()).name("Other Folder").please();
    Note target2 = makeMe.aNote("Target").folder(otherFolder).please();
    Note referrer =
        makeMe.aNote("Referrer").underSameNotebookAs(target1).content("See [[Target]].").please();
    controller.deleteNote(target2, leaveDeadLinksDeleteRequest());
    assertThat(
        controller.showNote(referrer).getWikiLinks().get(0).getResolution(),
        equalTo(WikiLink.Resolution.RESOLVED));

    controller.undoDeleteNote(target2);

    assertThat(
        controller.showNote(referrer).getWikiLinks().get(0).getResolution(),
        equalTo(WikiLink.Resolution.AMBIGUOUS));
  }
}
