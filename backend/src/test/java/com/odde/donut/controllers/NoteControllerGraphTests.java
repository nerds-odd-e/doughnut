package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.NoteReferenceService;
import com.odde.donut.services.focusContext.FocusContextInboundReference;
import com.odde.donut.services.focusContext.FocusContextResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NoteControllerGraphTests extends ControllerTestBase {
  @Autowired NoteController controller;
  @Autowired NoteReferenceService noteReferenceService;

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
  void relatedNotesExposeDepthAndPath() throws UnexpectedNoAccessRightException {
    makeMe.aNote("Linked").underSameNotebookAs(rootNote).content("See [[Root]]").please();

    FocusContextResult result = controller.getGraph(rootNote, 5000);
    assertThat(result.getRelatedNotes(), is(not(empty())));
    var related = result.getRelatedNotes().getFirst();
    assertThat(related.getDepth(), greaterThan(0));
    assertThat(related.getRetrievalPath(), is(notNullValue()));
  }

  @Test
  void inboundReferencesCarryTheReferringNotesPropertyTypes()
      throws UnexpectedNoAccessRightException {
    Note source = makeMe.aNote("Source").underSameNotebookAs(rootNote).please();
    makeMe.authorReferencingContent(
        source, "---\nprerequisite: \"[[Root]]\"\n---\nAlso [[Root]] in body.\n");
    noteReferenceService.refreshDerivedIndexesForNote(source);
    Note other = makeMe.aNote("Other").underSameNotebookAs(rootNote).please();
    makeMe.authorReferencingContent(other, "Links to [[Root]] in body.\n");

    List<FocusContextInboundReference> inbound =
        controller.getGraph(rootNote, 5000).getFocusNote().getInboundReferences();

    assertThat(inbound, hasSize(2));
    assertThat(inbound.get(0).referrerPropertyTypes(), contains("prerequisite"));
    assertThat(inbound.get(1).referrerPropertyTypes(), nullValue());
  }

  @Test
  void shouldNotAllowAccessToUnauthorizedNotes() {
    Note unauthorizedNote = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();
    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller.getGraph(unauthorizedNote, 5000));
  }
}
