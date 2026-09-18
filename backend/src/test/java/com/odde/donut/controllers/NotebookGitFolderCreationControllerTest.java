package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.controllers.dto.FolderCreationRequest;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.testability.GitBundleTestReader;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;

class NotebookGitFolderCreationControllerTest extends NotebookGitBundleControllerTestBase {

  @Test
  void webCreatedEmptyFolderAdvancesAcceptedHistoryWithMarker() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding before =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    ObjectId originalHead = ObjectId.fromString(before.getAcceptedGitObjectId());
    FolderCreationRequest request = new FolderCreationRequest();
    request.setName("Biology");

    Folder created = controller.createFolder(notebook, request);

    assertThat(created.getName(), is("Biology"));
    NotebookGitBinding after =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId acceptedHead = GitBundleTestReader.fetchHead(repository, after.getBundleBytes());
      assertThat(GitBundleTestReader.pathsIn(repository, acceptedHead), contains("Biology/.keep"));
      assertThat(
          repository
              .open(GitBundleTestReader.blobIdAt(repository, acceptedHead, "Biology/.keep"))
              .getBytes()
              .length,
          is(0));
      try (RevWalk revWalk = new RevWalk(repository)) {
        RevCommit acceptedCommit = revWalk.parseCommit(acceptedHead);
        assertThat(acceptedCommit.getParentCount(), is(1));
        assertThat(acceptedCommit.getParent(0).getId(), equalTo(originalHead));
      }
    }
  }

  @Test
  void nonGitNotebookFolderCreationCreatesNoBinding() throws Exception {
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    FolderCreationRequest request = new FolderCreationRequest();
    request.setName("Biology");

    Folder created = controller.createFolder(notebook, request);

    assertThat(created.getName(), is("Biology"));
    assertThat(countFoldersForNotebook(notebook.getId()), is(1L));
    assertThat(
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).isPresent(), is(false));
  }

  @Test
  void driftedNotebookFolderCreationKeepsMutationAndAcceptedHistoryUnchanged() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding before =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    String acceptedHeadBefore = before.getAcceptedGitObjectId();
    byte[] acceptedBundleBefore = before.getBundleBytes().clone();
    makeMe
        .aNote()
        .notebook(notebook)
        .title("Unsynchronized")
        .content("---\ntype: Note\n---\nbody")
        .please();
    long originalFolderCount = countFoldersForNotebook(notebook.getId());
    FolderCreationRequest request = new FolderCreationRequest();
    request.setName("Biology");

    Folder created = controller.createFolder(notebook, request);

    assertThat(created.getName(), is("Biology"));
    assertThat(countFoldersForNotebook(notebook.getId()), is(originalFolderCount + 1));
    NotebookGitBinding after =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    assertThat(after.getAcceptedGitObjectId(), is(acceptedHeadBefore));
    assertThat(after.getBundleBytes(), equalTo(acceptedBundleBefore));
  }

  @Test
  void nestedEmptyFolderReplacesParentMarkerWithoutLosingHistory() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    ObjectId originalHead =
        ObjectId.fromString(
            notebookGitBindingRepository
                .findByNotebook_Id(notebook.getId())
                .orElseThrow()
                .getAcceptedGitObjectId());
    FolderCreationRequest parentRequest = new FolderCreationRequest();
    parentRequest.setName("Science");
    Folder science = controller.createFolder(notebook, parentRequest);
    ObjectId parentHead =
        ObjectId.fromString(
            notebookGitBindingRepository
                .findByNotebook_Id(notebook.getId())
                .orElseThrow()
                .getAcceptedGitObjectId());
    FolderCreationRequest childRequest = new FolderCreationRequest();
    childRequest.setName("Biology");
    childRequest.setUnderFolderId(science.getId());

    Folder biology = controller.createFolder(notebook, childRequest);

    assertThat(biology.getParentFolder().getId(), equalTo(science.getId()));
    byte[] downloaded = controller.downloadNotebookGitBundle(notebook).getBody();
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId acceptedHead = GitBundleTestReader.fetchHead(repository, downloaded);
      assertThat(
          GitBundleTestReader.pathsIn(repository, acceptedHead), contains("Science/Biology/.keep"));
      try (RevWalk revWalk = new RevWalk(repository)) {
        RevCommit acceptedCommit = revWalk.parseCommit(acceptedHead);
        assertThat(acceptedCommit.getParent(0).getId(), equalTo(parentHead));
        assertThat(revWalk.parseCommit(parentHead).getParent(0).getId(), equalTo(originalHead));
      }
    }
  }
}
