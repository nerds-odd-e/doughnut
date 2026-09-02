package com.odde.donut.controllers;

import static com.odde.donut.entities.repositories.AuthoredNoteReferenceRowTestSupport.rowsFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.AuthoredNoteReferenceRow;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Live resolution after a target identity change must not rewrite unrelated source-owned note
 * reference rows.
 */
class TextContentControllerTargetIdentityLiveResolutionTest extends TextContentControllerTestBase {

  @Autowired NoteController noteController;
  @Autowired EntityManager entityManager;

  @Test
  void addingNamesakeLiveUpdatesOnlyTheAffectedReferenceWhileSourceRowsStayPut()
      throws UnexpectedNoAccessRightException {
    Note stable = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("Stable").please();
    Note target = makeMe.aNote().underSameNotebookAs(stable).title("Target").please();
    Note carrier = makeMe.aNote().underSameNotebookAs(stable).please();
    Folder otherFolder =
        makeMe.aFolder().notebook(target.getNotebook()).name("Other Folder").please();
    controller.updateNoteContent(carrier, contentDto("See [[Target]] and [[Stable]]."));

    List<String> authoredLinksBeforeNamesake =
        rowsFor(entityManager, carrier).stream()
            .map(AuthoredNoteReferenceRow::getAuthoredLink)
            .toList();
    assertThat(authoredLinksBeforeNamesake, contains("Target", "Stable"));
    assertThat(
        noteController.showNote(carrier).getWikiLinks().stream()
            .map(WikiLink::getResolution)
            .toList(),
        contains(WikiLink.Resolution.RESOLVED, WikiLink.Resolution.RESOLVED));

    makeMe.aNote().folder(otherFolder).title("Target").please();

    assertThat(
        rowsFor(entityManager, carrier).stream()
            .map(AuthoredNoteReferenceRow::getAuthoredLink)
            .toList(),
        equalTo(authoredLinksBeforeNamesake));
    assertThat(noteController.showNote(carrier).getWikiLinks(), hasSize(2));
    assertThat(
        noteController.showNote(carrier).getWikiLinks().stream()
            .filter(link -> "Stable".equals(link.getAuthoredLink()))
            .findFirst()
            .orElseThrow()
            .getResolution(),
        equalTo(WikiLink.Resolution.RESOLVED));
    assertThat(
        noteController.showNote(carrier).getWikiLinks().stream()
            .filter(link -> "Target".equals(link.getAuthoredLink()))
            .findFirst()
            .orElseThrow()
            .getResolution(),
        equalTo(WikiLink.Resolution.AMBIGUOUS));
  }
}
