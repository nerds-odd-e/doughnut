package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.NoteCreationDTO;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.testability.GitBundleTestReader;
import java.io.IOException;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;

class NotebookGitNoteCreationControllerTest extends NotebookGitBundleControllerTestBase {

  @Test
  void missingBindingKeepsTitleOnlyWebCreationWithoutAcquiringABinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    notebookGitBindingRepository.delete(
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow());

    NoteRealm result = controller.createNoteAtNotebookRoot(notebook, titleOnly("Unbound"));

    Note created = noteRepository.findById(result.getId()).orElseThrow();
    assertThat(created.getTitle(), is("Unbound"));
    assertThat(created.getFolder(), nullValue());
    assertThat(
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).isEmpty(), is(true));
  }

  @Test
  void folderDestinationKeepsExistingWebCreationAndAcceptedHead() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder folder = makeMe.aFolder().notebook(notebook).name("Box").please();
    NotebookGitBinding accepted = binding(notebook);
    NoteCreationDTO creation = titleOnly("Nested");
    creation.setFolderId(folder.getId());

    NoteRealm result = controller.createNoteAtNotebookRoot(notebook, creation);

    Note created = noteRepository.findById(result.getId()).orElseThrow();
    assertThat(created.getFolder().getId(), is(folder.getId()));
    assertBindingUnchanged(notebook, accepted);
  }

  @Test
  void initialContentKeepsExistingWebCreationAndAcceptedHead() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding accepted = binding(notebook);
    NoteCreationDTO creation = titleOnly("With Body");
    creation.setContent("# Hello");

    NoteRealm result = controller.createNoteAtNotebookRoot(notebook, creation);

    Note created = noteRepository.findById(result.getId()).orElseThrow();
    assertThat(created.getContent(), containsString("# Hello"));
    assertBindingUnchanged(notebook, accepted);
  }

  @Test
  void matchingAcceptedContentAppendsOnlyTheNewTitleOnlyRootFile() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("Existing").please();
    NotebookGitBinding accepted = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(accepted.getAcceptedGitObjectId());

    NoteRealm result = controller.createNoteAtNotebookRoot(notebook, titleOnly("Another"));

    Note created = noteRepository.findById(result.getId()).orElseThrow();
    assertThat(created.getTitle(), is("Another"));

    byte[] downloaded =
        controller
            .downloadNotebookGitBundle(notebookRepository.findById(notebook.getId()).orElseThrow())
            .getBody();
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId newHead = GitBundleTestReader.fetchHead(repository, downloaded);
      try (RevWalk revWalk = new RevWalk(repository)) {
        RevCommit commit = revWalk.parseCommit(newHead);
        assertThat(commit.getParentCount(), is(1));
        assertThat(commit.getParent(0).getId(), is(acceptedHead));
        assertThat(revWalk.parseCommit(acceptedHead).getId(), is(acceptedHead));
      }
      assertThat(
          GitBundleTestReader.pathsIn(repository, newHead),
          containsInAnyOrder("Another.md", "Existing.md"));
      assertThat(
          blobIdAt(repository, newHead, "Existing.md"),
          is(blobIdAt(repository, acceptedHead, "Existing.md")));
    }
  }

  @Test
  void earlierProjectionDriftKeepsExistingWebCreationAndAcceptedHead() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("Unsynchronized").please();
    NotebookGitBinding accepted = binding(notebook);

    NoteRealm result = controller.createNoteAtNotebookRoot(notebook, titleOnly("Another"));

    Note created = noteRepository.findById(result.getId()).orElseThrow();
    assertThat(created.getTitle(), is("Another"));
    assertBindingUnchanged(notebook, accepted);

    byte[] downloaded =
        controller
            .downloadNotebookGitBundle(notebookRepository.findById(notebook.getId()).orElseThrow())
            .getBody();
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId head = GitBundleTestReader.fetchHead(repository, downloaded);
      assertThat(GitBundleTestReader.pathsIn(repository, head), not(hasItem("Another.md")));
      assertThat(
          GitBundleTestReader.fetchAdvertisedHead(downloaded).getName(),
          is(accepted.getAcceptedGitObjectId()));
    }
  }

  @Test
  void unauthorizedCreationLeavesAcceptedHeadUnchanged() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding accepted = binding(notebook);
    User owner = currentUser.getUser();

    currentUser.setUser(createFixtureUser());
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.createNoteAtNotebookRoot(notebook, titleOnly("Intruder")));
    currentUser.setUser(owner);

    assertBindingUnchanged(notebook, accepted);
  }

  @Test
  void reservedTitleLeavesAcceptedHeadUnchanged() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding accepted = binding(notebook);

    ApiException ex =
        assertThrows(
            ApiException.class,
            () -> controller.createNoteAtNotebookRoot(notebook, titleOnly("readme")));
    assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.BINDING_ERROR));
    assertBindingUnchanged(notebook, accepted);
  }

  @Test
  void occupiedTitleLeavesAcceptedHeadUnchanged() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("Taken").please();
    NotebookGitBinding accepted = binding(notebook);

    assertThrows(
        ConstraintViolationException.class,
        () -> controller.createNoteAtNotebookRoot(notebook, titleOnly("Taken")));
    assertBindingUnchanged(notebook, accepted);
  }

  private static NoteCreationDTO titleOnly(String title) {
    NoteCreationDTO dto = new NoteCreationDTO();
    dto.setNewTitle(title);
    return dto;
  }

  private NotebookGitBinding binding(Notebook notebook) {
    return notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
  }

  private void assertBindingUnchanged(Notebook notebook, NotebookGitBinding before) {
    NotebookGitBinding after = binding(notebook);
    assertThat(after.getAcceptedGitObjectId(), is(before.getAcceptedGitObjectId()));
    assertThat(after.getBundleBytes(), equalTo(before.getBundleBytes()));
    assertThat(after.getUpdatedAt(), is(before.getUpdatedAt()));
  }

  private static ObjectId blobIdAt(Repository repository, ObjectId commitId, String path)
      throws IOException {
    try (RevWalk revWalk = new RevWalk(repository);
        TreeWalk treeWalk =
            TreeWalk.forPath(repository, path, revWalk.parseCommit(commitId).getTree())) {
      if (treeWalk == null) {
        throw new IOException("missing path: " + path);
      }
      return treeWalk.getObjectId(0);
    }
  }
}
