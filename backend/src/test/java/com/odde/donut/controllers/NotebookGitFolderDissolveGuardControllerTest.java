package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.FolderMoveRequest;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class NotebookGitFolderDissolveGuardControllerTest extends NotebookGitWebContentControllerTestBase {

  @Autowired FolderRepository folderRepository;

  @Test
  void folderContainingAFileCannotBeDissolved() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder physics = makeMe.aFolder().notebook(notebook).name("physics").please();
    Folder old = makeMe.aFolder().parentFolder(physics).name("old").please();
    NotebookAttachment sketch =
        storeFolderAttachmentAndSnapshot(notebook, old, "sketch.png", new byte[] {1, 2, 3});
    ObjectId acceptedA = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> folderController.dissolveFolder(notebook, old, false));

    assertThat(
        exception.getReason(),
        equalTo(
            "Folders containing files cannot be dissolved, merged, or moved to another notebook"
                + " yet."));
    assertThat(
        folderRepository.findById(old.getId()).orElseThrow().getParentFolder().getId(),
        equalTo(physics.getId()));
    assertThat(
        notebookAttachmentRepository.findById(sketch.getId()).orElseThrow().getFolder().getId(),
        equalTo(old.getId()));
    assertThat(ObjectId.fromString(binding(notebook).getAcceptedGitObjectId()), equalTo(acceptedA));
  }

  @Test
  void folderContainingAFileCannotBeMerged() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder target = makeMe.aFolder().notebook(notebook).name("physics").please();
    Folder holder = makeMe.aFolder().notebook(notebook).name("holder").please();
    Folder source = makeMe.aFolder().parentFolder(holder).name("physics").please();
    Folder old = makeMe.aFolder().parentFolder(source).name("old").please();
    NotebookAttachment sketch =
        storeFolderAttachmentAndSnapshot(notebook, old, "sketch.png", new byte[] {1, 2, 3});
    ObjectId acceptedA = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());
    FolderMoveRequest mergeAtRoot = new FolderMoveRequest();
    mergeAtRoot.setMerge(true);

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> folderController.moveFolder(notebook, source, mergeAtRoot));

    assertThat(
        exception.getReason(),
        equalTo(
            "Folders containing files cannot be dissolved, merged, or moved to another notebook"
                + " yet."));
    assertThat(
        folderRepository.findById(target.getId()).orElseThrow().getParentFolder(), nullValue());
    assertThat(
        folderRepository.findById(source.getId()).orElseThrow().getParentFolder().getId(),
        equalTo(holder.getId()));
    assertThat(
        folderRepository.findById(old.getId()).orElseThrow().getParentFolder().getId(),
        equalTo(source.getId()));
    assertThat(
        notebookAttachmentRepository.findById(sketch.getId()).orElseThrow().getFolder().getId(),
        equalTo(old.getId()));
    assertThat(ObjectId.fromString(binding(notebook).getAcceptedGitObjectId()), equalTo(acceptedA));
  }

  @Test
  void dissolveOntoANoteNameTakenIgnoringCaseIsRefusedNamingItAndChangesNothing() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder physics = makeMe.aFolder().notebook(notebook).name("physics").please();
    makeMe.aNote("Energy").folder(physics).please();
    Folder old = makeMe.aFolder().parentFolder(physics).name("old").please();
    Note energy = makeMe.aNote("energy").folder(old).please();
    snapshotCurrentPortableTree(notebook);
    ObjectId acceptedA = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());

    ApiException conflict =
        assertThrows(
            ApiException.class, () -> folderController.dissolveFolder(notebook, old, false));

    assertThat(
        conflict.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.RESOURCE_CONFLICT));
    assertThat(
        conflict.getErrorBody().getMessage(),
        equalTo("This name is already used here by physics/Energy.md"));
    assertThat(
        folderRepository.findById(old.getId()).orElseThrow().getParentFolder().getId(),
        equalTo(physics.getId()));
    assertThat(
        noteRepository.findById(energy.getId()).orElseThrow().getFolder().getId(),
        equalTo(old.getId()));
    assertThat(ObjectId.fromString(binding(notebook).getAcceptedGitObjectId()), equalTo(acceptedA));
  }

  @Test
  void mergeMoveOntoANestedNoteNameTakenIgnoringCaseIsRefusedNamingItAndChangesNothing()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder target = makeMe.aFolder().notebook(notebook).name("physics").please();
    Folder targetDiagrams = makeMe.aFolder().parentFolder(target).name("diagrams").please();
    makeMe.aNote("Energy").folder(targetDiagrams).please();
    Folder holder = makeMe.aFolder().notebook(notebook).name("holder").please();
    Folder source = makeMe.aFolder().parentFolder(holder).name("physics").please();
    Folder sourceDiagrams = makeMe.aFolder().parentFolder(source).name("Diagrams").please();
    Note energy = makeMe.aNote("energy").folder(sourceDiagrams).please();
    snapshotCurrentPortableTree(notebook);
    ObjectId acceptedA = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());
    FolderMoveRequest mergeAtRoot = new FolderMoveRequest();
    mergeAtRoot.setMerge(true);

    ApiException conflict =
        assertThrows(
            ApiException.class, () -> folderController.moveFolder(notebook, source, mergeAtRoot));

    assertThat(
        conflict.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.RESOURCE_CONFLICT));
    assertThat(
        conflict.getErrorBody().getMessage(),
        equalTo("This name is already used here by physics/diagrams/Energy.md"));
    assertThat(
        folderRepository.findById(source.getId()).orElseThrow().getParentFolder().getId(),
        equalTo(holder.getId()));
    assertThat(
        folderRepository.findById(sourceDiagrams.getId()).orElseThrow().getParentFolder().getId(),
        equalTo(source.getId()));
    assertThat(
        noteRepository.findById(energy.getId()).orElseThrow().getFolder().getId(),
        equalTo(sourceDiagrams.getId()));
    assertThat(ObjectId.fromString(binding(notebook).getAcceptedGitObjectId()), equalTo(acceptedA));
  }

  @Test
  void conflictingSiblingNameWithoutMergeLeavesFolderAndAcceptedHistoryUnchanged()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder outer = makeMe.aFolder().notebook(notebook).name("Outer").please();
    makeMe.aFolder().parentFolder(outer).name("Inner").please();
    Folder mid = makeMe.aFolder().parentFolder(outer).name("Mid").please();
    makeMe.aFolder().parentFolder(mid).name("Inner").please();
    snapshotCurrentPortableTree(notebook);
    ObjectId acceptedA = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());

    ApiException conflict =
        assertThrows(
            ApiException.class, () -> folderController.dissolveFolder(notebook, mid, false));

    assertThat(
        conflict.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.FOLDER_NAME_CONFLICT));
    assertThat(folderRepository.findById(mid.getId()).isPresent(), is(true));
    assertThat(ObjectId.fromString(binding(notebook).getAcceptedGitObjectId()), equalTo(acceptedA));
  }

  @Test
  void unauthorizedDissolveLeavesFolderAndAcceptedHistoryUnchanged() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    snapshotCurrentPortableTree(notebook);
    ObjectId acceptedA = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());
    User otherUser = createFixtureUser();
    currentUser.setUser(otherUser);

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> folderController.dissolveFolder(notebook, biology, false));

    assertThat(folderRepository.findById(biology.getId()).isPresent(), is(true));
    assertThat(ObjectId.fromString(binding(notebook).getAcceptedGitObjectId()), equalTo(acceptedA));
  }

  @Test
  void wrongNotebookDissolveLeavesFolderAndAcceptedHistoryUnchanged() throws Exception {
    Notebook owningNotebook = createGitBackedNotebook("Owning Notebook");
    Notebook otherNotebook = createGitBackedNotebook("Other Notebook");
    Folder biology = makeMe.aFolder().notebook(owningNotebook).name("Biology").please();
    snapshotCurrentPortableTree(owningNotebook);
    ObjectId acceptedA = ObjectId.fromString(binding(owningNotebook).getAcceptedGitObjectId());

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> folderController.dissolveFolder(otherNotebook, biology, false));

    assertThat(ex.getReason(), equalTo("Folder not in notebook."));
    assertThat(folderRepository.findById(biology.getId()).isPresent(), is(true));
    assertThat(
        ObjectId.fromString(binding(owningNotebook).getAcceptedGitObjectId()), equalTo(acceptedA));
  }

  @Test
  void nonGitNotebookFolderDissolveCreatesNoBinding() throws UnexpectedNoAccessRightException {
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    Folder outer = makeMe.aFolder().notebook(notebook).name("Outer").please();
    Folder biology = makeMe.aFolder().parentFolder(outer).name("Biology").please();

    folderController.dissolveFolder(notebook, biology, false);

    assertThat(folderRepository.findById(biology.getId()).isPresent(), is(false));
    assertThat(
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).isPresent(), is(false));
  }
}
