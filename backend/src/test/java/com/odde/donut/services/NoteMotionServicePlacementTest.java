package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.testability.MakeMe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteMotionServicePlacementTest {

  @Autowired private MakeMe makeMe;
  @Autowired private NoteMotionService noteMotionService;

  @Test
  void availablePlacementPreservesNoteAndDependentIdentity() {
    User owner = makeMe.aUser().please();
    Note note = makeMe.aNote("Subject").notebookOwnedBy(owner).please();
    MemoryTracker tracker = makeMe.aMemoryTrackerFor(note).please();
    Integer noteId = note.getId();
    Integer trackerId = tracker.getId();
    Folder destination = makeMe.aFolder().notebook(note.getNotebook()).name("Destination").please();

    noteMotionService.executeMoveIntoFolderWithAvailableTitle(note, destination);

    makeMe.refresh(note);
    makeMe.refresh(tracker);
    assertThat(note.getId(), equalTo(noteId));
    assertThat(tracker.getId(), equalTo(trackerId));
    assertThat(note.getFolder().getId(), equalTo(destination.getId()));
    assertThat(tracker.getNote().getId(), equalTo(note.getId()));
  }

  @Test
  void availablePlacementUsesTheFirstFreeNumberedSuffixIncludingAGap() {
    Note source = makeMe.aNote("Subject").please();
    Folder destination =
        makeMe.aFolder().notebook(source.getNotebook()).name("Destination").please();
    makeMe.aNote("subject").folder(destination).please();
    makeMe.aNote("SUBJECT (3)").folder(destination).please();

    noteMotionService.executeMoveIntoFolderWithAvailableTitle(source, destination);

    assertThat(source.getTitle(), equalTo("Subject (2)"));
  }

  @Test
  void availablePlacementShortensOnlyTheAllocatedTitleToFitTheSuffix() {
    String originalTitle = "x".repeat(Note.MAX_TITLE_LENGTH);
    Note source = makeMe.aNote(originalTitle).please();
    Folder destination =
        makeMe.aFolder().notebook(source.getNotebook()).name("Destination").please();
    makeMe.aNote(originalTitle).folder(destination).please();

    noteMotionService.executeMoveIntoFolderWithAvailableTitle(source, destination);

    assertThat(source.getTitle(), equalTo("x".repeat(Note.MAX_TITLE_LENGTH - 4) + " (2)"));
  }

  @Test
  void exactPlacementReversesTheSuppliedTitleAndLocation() {
    Note source = makeMe.aNote("Subject").please();
    Notebook notebook = source.getNotebook();
    Folder originalFolder = makeMe.aFolder().notebook(notebook).name("Original").please();
    Folder destination = makeMe.aFolder().notebook(notebook).name("Destination").please();
    noteMotionService.executePlacement(source, notebook, originalFolder, "Original title");
    makeMe.aNote("Original title").folder(destination).please();
    noteMotionService.executeMoveIntoFolderWithAvailableTitle(source, destination);

    noteMotionService.executePlacement(source, notebook, originalFolder, "Original title");

    assertThat(source.getTitle(), equalTo("Original title"));
    assertThat(source.getFolder().getId(), equalTo(originalFolder.getId()));
  }

  @Test
  void exactPlacementConflictLeavesTheSourceUnchanged() {
    Note source = makeMe.aNote("Original title").content("Original content").please();
    Notebook notebook = source.getNotebook();
    Folder originalFolder = makeMe.aFolder().notebook(notebook).name("Original").please();
    Folder destination = makeMe.aFolder().notebook(notebook).name("Destination").please();
    noteMotionService.executePlacement(source, notebook, originalFolder, source.getTitle());
    makeMe.aNote("Occupied").folder(destination).please();

    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> noteMotionService.executePlacement(source, notebook, destination, "Occupied"));

    assertThat(
        exception.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.RESOURCE_CONFLICT));
    assertThat(source.getTitle(), equalTo("Original title"));
    assertThat(source.getContent(), equalTo("Original content"));
    assertThat(source.getFolder().getId(), equalTo(originalFolder.getId()));
  }
}
