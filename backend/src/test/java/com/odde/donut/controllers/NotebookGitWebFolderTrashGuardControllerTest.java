package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookGitWebFolderTrashGuardControllerTest extends NotebookGitWebContentControllerTestBase {
  static final String CELLS_BODY = "---\ntype: Note\n---\ncells body";

  @Autowired FolderRepository folderRepository;

  @Test
  void unauthorizedTrashLeavesFolderAndAcceptedBindingUnchanged() throws Exception {
    GuardFixture f = seedBiologyUnderResearch();
    CommittedFolderAndBinding setup =
        committedFolderAndBinding(f.biology().getId(), f.notebook().getId());
    User otherUser = createFixtureUser();
    currentUser.setUser(otherUser);

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.trashFolder(f.notebook(), f.biology()));

    CommittedFolderAndBinding committed =
        committedFolderAndBinding(f.biology().getId(), f.notebook().getId());
    assertThat(committed.parentFolderId(), equalTo(f.research().getId()));
    assertThat(committed.trashed(), is(false));
    assertThat(committed.acceptedHead(), equalTo(setup.acceptedHead()));
    assertThat(committed.acceptedBundle(), equalTo(setup.acceptedBundle()));
  }

  @Test
  void preExistingPortableDriftKeepsTheFolderTrashAndAcceptedHistoryUnchanged() throws Exception {
    GuardFixture f = seedBiologyUnderResearch();
    ObjectId acceptedA = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    byte[] acceptedBundle = binding(f.notebook()).getBundleBytes();
    makeMe.aNote().notebook(f.notebook()).title("Unsynchronized").content(CELLS_BODY).please();

    controller.trashFolder(f.notebook(), f.biology());

    assertThat(
        parentFolderName(parentFolderId(parentFolderId(f.biology().getId()))), equalTo("_trash"));
    assertThat(
        ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId()), equalTo(acceptedA));
    assertThat(binding(f.notebook()).getBundleBytes(), equalTo(acceptedBundle));
  }

  GuardFixture seedBiologyUnderResearch() throws UnexpectedNoAccessRightException {
    Notebook notebook = createGitBackedNotebook();
    Folder research = makeMe.aFolder().notebook(notebook).name("Research").please();
    Folder biology = makeMe.aFolder().parentFolder(research).name("Biology").please();
    makeMe.aNote("Cells").folder(biology).content(CELLS_BODY).please();
    snapshotCurrentPortableTree(notebook);
    return new GuardFixture(notebook, research, biology);
  }

  Integer parentFolderId(Integer folderId) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          Folder folder = folderRepository.findById(folderId).orElseThrow();
          return folder.getParentFolder() == null ? null : folder.getParentFolder().getId();
        });
  }

  String parentFolderName(Integer folderId) {
    return inCommittedTransaction(
        transactionManager, () -> folderRepository.findById(folderId).orElseThrow().getName());
  }

  CommittedFolderAndBinding committedFolderAndBinding(Integer folderId, Integer notebookId) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          Folder folder = folderRepository.findById(folderId).orElseThrow();
          NotebookGitBinding binding =
              notebookGitBindingRepository.findByNotebook_Id(notebookId).orElseThrow();
          return new CommittedFolderAndBinding(
              folder.getParentFolder() == null ? null : folder.getParentFolder().getId(),
              folder.isTrashed(),
              binding.getAcceptedGitObjectId(),
              binding.getBundleBytes().clone());
        });
  }

  record GuardFixture(Notebook notebook, Folder research, Folder biology) {}

  record CommittedFolderAndBinding(
      Integer parentFolderId, boolean trashed, String acceptedHead, byte[] acceptedBundle) {}
}
