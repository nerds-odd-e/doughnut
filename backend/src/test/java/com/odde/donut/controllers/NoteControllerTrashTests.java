package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.NoteDeleteDTO;
import com.odde.donut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.NoteReferenceService;
import com.odde.donut.services.httpQuery.HttpClientAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NoteControllerTrashTests extends ControllerTestBase {
  @Autowired NoteController controller;
  @Autowired MemoryTrackerController memoryTrackerController;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;
  @Autowired NoteReferenceService noteReferenceService;
  @Autowired TextContentController textContentController;
  @MockitoBean HttpClientAdapter httpClientAdapter;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void rejectsTrashForAnotherOwnersNote() {
    Note note = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();

    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller.trashNote(note, leaveDeadLinks()));
  }

  @Test
  void trashesAndUndoesALearnedNoteWhileRetainingIdentityHistoryAndPreferences()
      throws UnexpectedNoAccessRightException {
    Folder originalFolder =
        makeMe
            .aFolder()
            .notebook(makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please())
            .name("Topics")
            .please();
    Note note = makeMe.aNote("Subject").folder(originalFolder).please();
    MemoryTracker learned = makeMe.aMemoryTrackerFor(note).please();
    makeMe.aRecallLogFor(learned).please();
    MemoryTracker stopped =
        makeMe.aMemoryTrackerFor(note).spelling().removedFromTracking().please();
    Integer noteId = note.getId();
    Integer learnedId = learned.getId();
    Integer stoppedId = stopped.getId();

    NoteRealm trashed = controller.trashNote(note, leaveDeadLinks());

    assertThat(trashed.getNote().getId(), equalTo(noteId));
    assertThat(trashed.getNote().isTrashed(), equalTo(true));
    assertThat(note.getFolder().getName(), equalTo("Topics"));
    assertThat(note.getFolder().getParentFolder().getName(), equalTo("_trash"));

    NoteRealm restored = controller.undoTrashNote(note, undoTo("Subject", originalFolder));

    assertThat(restored.getNote().getId(), equalTo(noteId));
    assertThat(restored.getNote().isTrashed(), equalTo(false));
    assertThat(note.getTitle(), equalTo("Subject"));
    assertThat(note.getFolder().getId(), equalTo(originalFolder.getId()));
    assertThat(memoryTrackerController.getRecallHistory(learned), hasSize(1));
    assertThat(
        memoryTrackerRepository.findById(learnedId).orElseThrow().getNote().getId(),
        equalTo(noteId));
    assertThat(
        memoryTrackerRepository.findById(stoppedId).orElseThrow().getRemovedFromTracking(),
        equalTo(true));
  }

  @Test
  void trashingAndUndoingPreservesAuthoredContentAndNoteIdentity()
      throws UnexpectedNoAccessRightException {
    Folder originalFolder =
        makeMe
            .aFolder()
            .notebook(makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please())
            .name("Topics")
            .please();
    Note note = makeMe.aNote("Subject").folder(originalFolder).please();
    NoteUpdateContentDTO content = new NoteUpdateContentDTO();
    content.setContent("---\ntype: Note\n---\nAuthored body that must survive trash");
    textContentController.updateNoteContent(note, content);
    Integer noteId = note.getId();
    String authoredContent = note.getContent();

    NoteRealm trashed = controller.trashNote(note, leaveDeadLinks());
    NoteRealm restored = controller.undoTrashNote(note, undoTo("Subject", originalFolder));

    assertThat(restored.getNote().getId(), equalTo(noteId));
    assertThat(restored.getNote().isTrashed(), equalTo(false));
    assertThat(note.getContent(), equalTo(authoredContent));
    assertThat(note.getFolder().getId(), equalTo(originalFolder.getId()));
  }

  @Test
  void repeatedTrashAfterNameReuseKeepsDistinctNotesAndUsesTheFirstFreeTrashTitle()
      throws UnexpectedNoAccessRightException {
    Note earlier = makeMe.aNote("Reusable").notebookOwnedBy(currentUser.getUser()).please();
    controller.trashNote(earlier, leaveDeadLinks());
    Note replacement = makeMe.aNote("Reusable").underSameNotebookAs(earlier).please();

    controller.trashNote(replacement, leaveDeadLinks());

    assertThat(earlier.getTitle(), equalTo("Reusable"));
    assertThat(replacement.getTitle(), equalTo("Reusable (2)"));
    assertThat(replacement.getId().equals(earlier.getId()), equalTo(false));
    assertThat(replacement.getFolder().getId(), equalTo(earlier.getFolder().getId()));

    controller.undoTrashNote(replacement, undoTo("Reusable", null));

    assertThat(replacement.getTitle(), equalTo("Reusable"));
    assertThat(replacement.getFolder(), nullValue());
    assertThat(earlier.isTrashed(), equalTo(true));
  }

  @Test
  void occupiedUndoDestinationLeavesTheNoteInTrash() throws UnexpectedNoAccessRightException {
    Note note = makeMe.aNote("Occupied").notebookOwnedBy(currentUser.getUser()).please();
    controller.trashNote(note, leaveDeadLinks());
    Folder trashParent = note.getFolder();
    makeMe.aNote("Occupied").underSameNotebookAs(note).please();

    ApiException exception =
        assertThrows(
            ApiException.class, () -> controller.undoTrashNote(note, undoTo("Occupied", null)));

    assertThat(
        exception.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.RESOURCE_CONFLICT));
    assertThat(note.getTitle(), equalTo("Occupied"));
    assertThat(note.getFolder().getId(), equalTo(trashParent.getId()));
    assertThat(note.isTrashed(), equalTo(true));
  }

  @Test
  void rejectsUndoToAnotherOwnersDestinationAndLeavesTheNoteInTrash()
      throws UnexpectedNoAccessRightException {
    Note note = makeMe.aNote("Subject").notebookOwnedBy(currentUser.getUser()).please();
    controller.trashNote(note, leaveDeadLinks());
    Folder trashParent = note.getFolder();
    Folder foreignFolder =
        makeMe
            .aFolder()
            .notebook(makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please())
            .name("Foreign")
            .please();

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.undoTrashNote(note, undoTo("Subject", foreignFolder)));

    assertThat(note.getFolder().getId(), equalTo(trashParent.getId()));
    assertThat(note.isTrashed(), equalTo(true));
  }

  @Test
  void trashAppliesRemoveFromPropertiesReferenceChoice() throws UnexpectedNoAccessRightException {
    Note target = makeMe.aNote("Target").notebookOwnedBy(currentUser.getUser()).please();
    Note referrer = makeMe.aNote("Referrer").underSameNotebookAs(target).please();
    NoteUpdateContentDTO content = new NoteUpdateContentDTO();
    content.setContent("---\ntarget: \"[[Target]]\"\n---\nBody");
    textContentController.updateNoteContent(referrer, content);

    controller.trashNote(target, removeFromProperties());

    assertThat(referrer.getContent(), equalTo("---\ntype: Note\n---\nBody"));
    assertThat(target.isTrashed(), equalTo(true));
  }

  @Test
  void trashAppliesReduceToSourceReferenceChoiceWithoutSoftDeletingTheRelation()
      throws UnexpectedNoAccessRightException {
    Note source = makeMe.aNote("Moon").notebookOwnedBy(currentUser.getUser()).please();
    Note target = makeMe.aNote("Earth").underSameNotebookAs(source).please();
    Note relation =
        makeMe
            .aNote()
            .underSameNotebookAs(source)
            .asRelationship("a part of", source, target)
            .please();
    noteReferenceService.refreshDerivedIndexesForNote(relation);
    NoteDeleteDTO request = new NoteDeleteDTO();
    request.setReferenceHandling(NoteDeleteReferenceHandling.REDUCE_TO_SOURCE_PROPERTY);
    request.setSourcePropertyKey("a part of");

    controller.trashNote(relation, request);

    assertThat(source.getContent(), containsString("a part of"));
    assertThat(source.getContent(), containsString("[[Earth]]"));
    assertThat(relation.isTrashed(), equalTo(true));
  }
}
