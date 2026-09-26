package com.odde.donut.controllers;

import static com.odde.donut.entities.repositories.AuthoredNoteReferenceRowTestSupport.rowsFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.NoteCreationDTO;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookNoteCreateControllerTest extends NotebookControllerTestBase {

  @Autowired NoteController noteController;
  @Autowired TextContentController textContentController;
  @Autowired EntityManager entityManager;

  private static final String DUPLICATE_TITLE_MESSAGE =
      "A note with this title already exists in this notebook (folder or top level).";

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
  void preservesPipeTitleAcrossCreationAndSubsequentBodySave() throws Exception {
    Notebook notebook = ownedNotebook();
    Note fullwidth = makeMe.aNote().notebook(notebook).title("A｜B").please();

    NoteRealm created = controller.createNoteAtNotebookRoot(notebook, noteCreate("A|B"));
    NoteUpdateContentDTO content = new NoteUpdateContentDTO();
    content.setContent("Edited body");
    textContentController.updateNoteContent(created.getNote(), content);
    entityManager.flush();
    entityManager.clear();

    assertThat(noteRepository.findById(created.getId()).orElseThrow().getTitle(), equalTo("A|B"));
    assertThat(noteRepository.findById(fullwidth.getId()).orElseThrow().getTitle(), equalTo("A｜B"));
  }

  @Test
  void creatingRootNoteWithAuthoredReferencePersistsItsSourceIndexRow() throws Exception {
    NoteCreationDTO noteCreation = noteCreate("Carrier");
    noteCreation.setContent("[[Missing]]");
    NoteRealm result = controller.createNoteAtNotebookRoot(ownedNotebook(), noteCreation);

    Note created = noteRepository.findById(result.getId()).orElseThrow();
    assertThat(rowsFor(entityManager, created), hasSize(1));
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
    long noteCountBefore = noteRepository.count();

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.createNoteAtNotebookRoot(nb1, noteCreateInFolder("Intruding", f2)));

    assertThat(ex.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    assertThat(ex.getReason(), equalTo("Folder not in notebook."));
    assertThat(noteRepository.count(), equalTo(noteCountBefore));
  }

  @ParameterizedTest
  @CsvSource({
    "SameTitle, SameTitle",
    "SameTitle, sametitle",
  })
  void rejectsDuplicateTitleAtNotebookRoot(String existingTitle, String newTitle) {
    Notebook nb = ownedNotebook();
    makeMe.aNote().notebook(nb).title(existingTitle).please();
    ApiException ex =
        assertThrows(
            ApiException.class,
            () -> controller.createNoteAtNotebookRoot(nb, noteCreate(newTitle)));
    assertTitleRefused(ex, DUPLICATE_TITLE_MESSAGE);
  }

  @Test
  void rejectsDuplicateTitleInSameFolder() {
    Notebook nb = ownedNotebook();
    Folder folder = ownedFolder(nb, "F");
    makeMe.aNote().folder(folder).title("InFolder").please();
    ApiException ex =
        assertThrows(
            ApiException.class,
            () -> controller.createNoteAtNotebookRoot(nb, noteCreateInFolder("InFolder", folder)));
    assertTitleRefused(ex, DUPLICATE_TITLE_MESSAGE);
  }

  @Test
  void aFolderHoldingTheNoteFileNameIgnoringCaseRefusesTheCreateNamingIt() {
    Notebook nb = ownedNotebook();
    Folder physics = ownedFolder(nb, "physics");
    makeMe.aFolder().parentFolder(physics).name("energy.md").please();
    long noteCountBefore = noteRepository.count();

    ApiException ex =
        assertThrows(
            ApiException.class,
            () -> controller.createNoteAtNotebookRoot(nb, noteCreateInFolder("Energy", physics)));

    assertTitleRefused(ex, "This name is already used here by physics/energy.md/");
    assertThat(noteRepository.count(), equalTo(noteCountBefore));
  }

  private static void assertTitleRefused(ApiException ex, String message) {
    assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.RESOURCE_CONFLICT));
    assertThat(ex.getErrorBody().getErrors().get("newTitle"), equalTo(message));
  }

  @Test
  void creatingANamesakeMakesAnExistingUniqueShorthandAmbiguous() throws Exception {
    Notebook nb = ownedNotebook();
    Note target = makeMe.aNote().notebook(nb).title("Target").please();
    Note referrer = makeMe.aNote().underSameNotebookAs(target).content("See [[Target]].").please();
    Folder otherFolder = ownedFolder(nb, "Other Folder");
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
