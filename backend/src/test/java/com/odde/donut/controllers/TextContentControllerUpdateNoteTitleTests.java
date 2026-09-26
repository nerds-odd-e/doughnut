package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.configs.ObjectMapperConfig;
import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.donut.controllers.dto.TitleRenameReferenceHandling;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TextContentControllerUpdateNoteTitleTests extends TextContentControllerTestBase {
  @Autowired NoteController noteController;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  NoteUpdateTitleDTO noteUpdateTitleDTO = titleDto("new title");

  @Test
  void shouldBeAbleToSaveNoteTitle() throws UnexpectedNoAccessRightException {
    NoteRealm response = controller.updateNoteTitle(note, noteUpdateTitleDTO);
    assertThat(response.getId(), equalTo(note.getId()));
    assertThat(response.getNote().getTitle(), equalTo("new title"));
  }

  @Test
  void shouldPersistTitleWithoutSurroundingCrLfFromJson() throws Exception {
    ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();
    NoteUpdateTitleDTO titleDto =
        objectMapper.readValue("{\"newTitle\": \"\\r\\nAfter\\r\\n\"}", NoteUpdateTitleDTO.class);
    assertThat(controller.updateNoteTitle(note, titleDto).getNote().getTitle(), equalTo("After"));
  }

  @Test
  void renamingALearnedNoteToAPipeTitlePreservesItsIdentityAndLearning() throws Exception {
    Note learned = makeMe.aNote().title("Original").notebookOwnedBy(currentUser.getUser()).please();
    MemoryTracker tracker = makeMe.aMemoryTrackerFor(learned).recallCount(1).please();
    Note referrer = makeMe.aNote().underSameNotebookAs(learned).please();
    controller.updateNoteContent(referrer, contentDto("[[Original]]"));
    NoteUpdateTitleDTO titleDto = titleDto("A|B");
    titleDto.setReferenceHandling(TitleRenameReferenceHandling.UPDATE_VISIBLE_TEXT);

    NoteRealm response = controller.updateNoteTitle(learned, titleDto);

    assertThat(response.getNote().getId(), equalTo(learned.getId()));
    assertThat(response.getNote().getTitle(), equalTo("A|B"));
    MemoryTracker reloadedTracker = memoryTrackerRepository.findById(tracker.getId()).orElseThrow();
    assertThat(reloadedTracker.getNote().getId(), equalTo(learned.getId()));
    assertThat(reloadedTracker.getRecallCount(), equalTo(1));
  }

  @Test
  void shouldNotAllowOthersToChange() {
    Note other = makeMe.aNote("another").notebookOwnedBy(makeMe.aUser().please()).please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.updateNoteTitle(other, noteUpdateTitleDTO));
  }

  @Test
  void shouldReresolveNotebookShorthandsWhenRenameIntroducesOrRemovesACollision()
      throws UnexpectedNoAccessRightException {
    Note target = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("Target").please();
    Folder otherFolder =
        makeMe.aFolder().notebook(target.getNotebook()).name("Other Folder").please();
    Note referrer = makeMe.aNote().underSameNotebookAs(target).content("See [[Target]].").please();
    Note namesake = makeMe.aNote().folder(otherFolder).title("Other").please();
    assertThat(
        noteController.showNote(referrer).getWikiLinks().get(0).getResolution(),
        equalTo(WikiLink.Resolution.RESOLVED));

    controller.updateNoteTitle(namesake, titleDto("Target"));

    assertThat(
        noteController.showNote(referrer).getWikiLinks().get(0).getResolution(),
        equalTo(WikiLink.Resolution.AMBIGUOUS));

    controller.updateNoteTitle(namesake, titleDto("Other"));

    assertThat(
        noteController.showNote(referrer).getWikiLinks().get(0).getResolution(),
        equalTo(WikiLink.Resolution.RESOLVED));
  }

  @Test
  void aFolderHoldingTheNoteFileNameIgnoringCaseRefusesTheRenameNamingIt() {
    Folder physics =
        makeMe.aFolder().notebookOwnedBy(currentUser.getUser()).name("physics").please();
    makeMe.aFolder().parentFolder(physics).name("energy.md").please();
    Note work = makeMe.aNote().folder(physics).title("Work").please();

    ApiException ex =
        assertThrows(
            ApiException.class, () -> controller.updateNoteTitle(work, titleDto("Energy")));

    assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.RESOURCE_CONFLICT));
    assertThat(
        ex.getErrorBody().getErrors().get("newTitle"),
        equalTo("This name is already used here by physics/energy.md/"));
  }

  @Test
  void anotherNoteHoldingTheTitleIgnoringCaseRefusesTheRename() {
    makeMe.aNote().underSameNotebookAs(note).title("Taken").please();

    ApiException ex =
        assertThrows(ApiException.class, () -> controller.updateNoteTitle(note, titleDto("taken")));

    assertThat(
        ex.getErrorBody().getErrors().get("newTitle"),
        equalTo("A note with this title already exists in this notebook (folder or top level)."));
  }

  @Test
  void aCaseOnlyRenameOfTheSameNoteIsAllowed() throws UnexpectedNoAccessRightException {
    assertThat(
        controller.updateNoteTitle(note, titleDto("NEW")).getNote().getTitle(), equalTo("NEW"));
  }
}
