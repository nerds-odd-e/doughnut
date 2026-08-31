package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.NoteCreationDTO;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.ResolvedWikiLinkService;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookNoteCreateControllerTest extends NotebookControllerTestBase {

  @Autowired NoteController noteController;
  @Autowired ResolvedWikiLinkService resolvedWikiLinkService;

  private NoteCreationDTO noteCreate(String title) {
    NoteCreationDTO dto = new NoteCreationDTO();
    dto.setNewTitle(title);
    return dto;
  }

  private NoteCreationDTO noteCreateInFolder(String title, Folder folder) {
    NoteCreationDTO dto = noteCreate(title);
    dto.setFolderId(folder.getId());
    return dto;
  }

  @Test
  void createsTopLevelNoteWithNullParentFolder() throws Exception {
    Notebook nb = ownedNotebook();
    assertThat(noteRepository.findNotesInNotebookRootFolderScopeByNotebookId(nb.getId()), empty());

    NoteRealm result = controller.createNoteAtNotebookRoot(nb, noteCreate("Root One"));

    Note created = noteRepository.findById(result.getId()).orElseThrow();
    assertThat(created.getFolder(), nullValue());
    assertThat(created.getNotebook().getId(), equalTo(nb.getId()));
  }

  @Test
  void titleOnlyCreateStoresOrdinaryNoteTypeFence() throws Exception {
    NoteRealm result = controller.createNoteAtNotebookRoot(ownedNotebook(), noteCreate("Root One"));

    Note created = noteRepository.findById(result.getId()).orElseThrow();
    assertThat(created.getContent(), equalTo("---\ntype: Note\n---\n"));
  }

  @Test
  void persistsInitialMarkdownContentWhenProvided() throws Exception {
    NoteCreationDTO noteCreation = noteCreate("Root With Body");
    noteCreation.setContent("# Hello\n\n[[Link]]");
    NoteRealm result = controller.createNoteAtNotebookRoot(ownedNotebook(), noteCreation);

    Note created = noteRepository.findById(result.getId()).orElseThrow();
    assertThat(created.getContent(), equalTo("---\ntype: Note\n---\n# Hello\n\n[[Link]]"));
  }

  @Test
  void rejectsNotebookOwnedByAnotherUser() {
    Notebook nb = makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.createNoteAtNotebookRoot(nb, noteCreate("Intruder")));
  }

  @Test
  void createsNotesInFolderInAppendLastOrder() throws Exception {
    Notebook nb = ownedNotebook();
    Folder f = ownedFolder(nb, "Box");

    controller.createNoteAtNotebookRoot(nb, noteCreateInFolder("A", f));
    controller.createNoteAtNotebookRoot(nb, noteCreateInFolder("B", f));

    assertThat(
        noteRepository.findNotesInFolderOrderByIdAsc(f.getId()).stream()
            .map(Note::getTitle)
            .toList(),
        contains("A", "B"));
  }

  @Test
  void rejectsFolderIdFromAnotherNotebook() {
    Notebook nb1 = ownedNotebook();
    Folder f2 = ownedFolder(ownedNotebook(), "Other");
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.createNoteAtNotebookRoot(nb1, noteCreateInFolder("Intruding", f2)));
    assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
  }

  @ParameterizedTest
  @CsvSource({
    "SameTitle, SameTitle",
    "SameTitle, sametitle",
  })
  void rejectsDuplicateTitleAtNotebookRoot(String existingTitle, String newTitle) {
    Notebook nb = ownedNotebook();
    makeMe.aNote().notebook(nb).title(existingTitle).please();
    assertThrows(
        ConstraintViolationException.class,
        () -> controller.createNoteAtNotebookRoot(nb, noteCreate(newTitle)));
  }

  @Test
  void rejectsDuplicateTitleInSameFolder() {
    Notebook nb = ownedNotebook();
    Folder folder = ownedFolder(nb, "F");
    makeMe.aNote().folder(folder).title("InFolder").please();
    assertThrows(
        ConstraintViolationException.class,
        () -> controller.createNoteAtNotebookRoot(nb, noteCreateInFolder("InFolder", folder)));
  }

  @Test
  void creatingANamesakeMakesAnExistingUniqueShorthandAmbiguous() throws Exception {
    Notebook nb = ownedNotebook();
    Note target = makeMe.aNote().notebook(nb).title("Target").please();
    Note referrer = makeMe.aNote().underSameNotebookAs(target).content("See [[Target]].").please();
    Folder otherFolder = ownedFolder(nb, "Other Folder");
    resolvedWikiLinkService.refreshForNote(referrer, currentUser.getUser());
    assertThat(
        noteController.showNote(referrer).getWikiLinks().get(0).getResolution(),
        equalTo(WikiLink.Resolution.RESOLVED));

    controller.createNoteAtNotebookRoot(nb, noteCreateInFolder("Target", otherFolder));

    assertThat(
        noteController.showNote(referrer).getWikiLinks().get(0).getResolution(),
        equalTo(WikiLink.Resolution.AMBIGUOUS));
  }

  @ParameterizedTest
  @ValueSource(strings = {"readme", "README", "readme.md", "README.md"})
  void rejectsReservedReadmeTitleOnCreate(String reservedTitle) {
    ApiException ex =
        assertThrows(
            ApiException.class,
            () -> controller.createNoteAtNotebookRoot(ownedNotebook(), noteCreate(reservedTitle)));

    assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.BINDING_ERROR));
    assertThat(ex.getErrorBody().getErrors().get("newTitle"), containsString("reserved"));
  }
}
