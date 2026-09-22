package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.FolderRenameRequest;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class NotebookGitFolderRenameGuardControllerTest extends NotebookGitWebContentControllerTestBase {

  static final String CELLS_BODY = "---\ntype: Note\n---\ncells body";

  @Autowired FolderRepository folderRepository;

  @Test
  void conflictingSiblingNameLeavesFolderAndAcceptedHistoryUnchanged() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aFolder().notebook(notebook).name("Taken").please();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    snapshotCurrentPortableTree(notebook);
    ObjectId acceptedA = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());

    ApiException conflict =
        assertThrows(
            ApiException.class,
            () -> folderController.renameFolder(notebook, biology, renameTo("Taken")));

    assertThat(
        conflict.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.FOLDER_NAME_CONFLICT));
    assertThat(folderRepository.findById(biology.getId()).orElseThrow().getName(), is("Biology"));
    assertThat(ObjectId.fromString(binding(notebook).getAcceptedGitObjectId()), equalTo(acceptedA));
  }

  @Test
  void unauthorizedRenameLeavesFolderAndAcceptedHistoryUnchanged() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    snapshotCurrentPortableTree(notebook);
    ObjectId acceptedA = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());
    User otherUser = createFixtureUser();
    currentUser.setUser(otherUser);

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> folderController.renameFolder(notebook, biology, renameTo("Zoology")));

    assertThat(folderRepository.findById(biology.getId()).orElseThrow().getName(), is("Biology"));
    assertThat(ObjectId.fromString(binding(notebook).getAcceptedGitObjectId()), equalTo(acceptedA));
  }

  @Test
  void wrongNotebookRenameLeavesFolderAndAcceptedHistoryUnchanged() throws Exception {
    Notebook owningNotebook = createGitBackedNotebook("Owning Notebook");
    Notebook otherNotebook = createGitBackedNotebook("Other Notebook");
    Folder biology = makeMe.aFolder().notebook(owningNotebook).name("Biology").please();
    snapshotCurrentPortableTree(owningNotebook);
    ObjectId acceptedA = ObjectId.fromString(binding(owningNotebook).getAcceptedGitObjectId());

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> folderController.renameFolder(otherNotebook, biology, renameTo("Zoology")));

    assertThat(ex.getReason(), equalTo("Folder not in notebook."));
    assertThat(folderRepository.findById(biology.getId()).orElseThrow().getName(), is("Biology"));
    assertThat(
        ObjectId.fromString(binding(owningNotebook).getAcceptedGitObjectId()), equalTo(acceptedA));
  }

  @Test
  void preExistingPortableDriftDoesNotBlockTheFolderRename() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    snapshotCurrentPortableTree(notebook);
    var acceptedHistoryBefore = acceptedHistory(notebook);
    makeMe.aNote().notebook(notebook).title("Unsynchronized").content(CELLS_BODY).please();

    folderController.renameFolder(notebook, biology, renameTo("Zoology"));

    AcceptedHistory after = acceptedHistory(notebook);
    assertThat(after.parents(), equalTo(acceptedHistoryBefore.commits()));
    assertThat(after.tipPaths(), hasItem("Zoology/.keep"));
  }

  static FolderRenameRequest renameTo(String name) {
    FolderRenameRequest req = new FolderRenameRequest();
    req.setName(name);
    return req;
  }
}
