package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.NoteCreationDTO;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;

class NotebookGitNoteCreationControllerTest extends NotebookGitNoteCreationControllerTestSupport {

  private static final String CANONICAL_INITIAL_CONTENT =
      "---\ntype: Note\naliases:\n  - hello\n---\nSee [[Link]]\n";

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
  void initialOrdinaryMarkdownIsAcceptedAsCanonicalRootFile() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    ObjectId acceptedHead = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());
    NoteCreationDTO creation = titleOnly("With Body");
    creation.setContent("---\naliases:\n  - hello\n---\nSee [[Link]]\n");

    NoteRealm result = controller.createNoteAtNotebookRoot(notebook, creation);

    Note created = noteRepository.findById(result.getId()).orElseThrow();
    assertThat(created.getContent(), is(CANONICAL_INITIAL_CONTENT));

    byte[] downloaded = acceptedBundleBytes(notebook);
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId newHead = GitBundleTestReader.fetchHead(repository, downloaded);
      try (RevWalk revWalk = new RevWalk(repository)) {
        RevCommit commit = revWalk.parseCommit(newHead);
        assertThat(commit.getParentCount(), is(1));
        assertThat(commit.getParent(0).getId(), is(acceptedHead));
      }
      assertThat(GitBundleTestReader.pathsIn(repository, newHead), contains("With Body.md"));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repository, newHead, "With Body.md"),
          is(CANONICAL_INITIAL_CONTENT));
    }
  }

  @Test
  void relationshipNoteKeepsExistingWebCreationAndAcceptedHead() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    AcceptedBinding accepted = acceptedBinding(notebook);
    NoteCreationDTO creation = titleOnly("Relates");
    creation.setContent("---\ntype: Relationship\nsource: \"[[A]]\"\ntarget: \"[[B]]\"\n---\n");

    NoteRealm result = controller.createNoteAtNotebookRoot(notebook, creation);

    Note created = noteRepository.findById(result.getId()).orElseThrow();
    assertThat(created.getContent(), containsString("type: Relationship"));
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

    byte[] downloaded = acceptedBundleBytes(notebook);
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
          GitBundleTestReader.blobIdAt(repository, newHead, "Existing.md"),
          is(GitBundleTestReader.blobIdAt(repository, acceptedHead, "Existing.md")));
    }
  }

  @Test
  void preExistingPortableDriftDoesNotBlockTheNoteCreation() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("Unsynchronized").please();
    var acceptedHistoryBefore = acceptedHistory(notebook);

    controller.createNoteAtNotebookRoot(notebook, titleOnly("Another"));

    AcceptedHistory after = acceptedHistory(notebook);
    assertThat(after.parents(), equalTo(acceptedHistoryBefore.commits()));
    assertThat(after.tipPaths(), hasItem("Another.md"));
  }

  @Test
  void unauthorizedCreationLeavesAcceptedHeadUnchanged() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    AcceptedBinding accepted = acceptedBinding(notebook);
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
    AcceptedBinding accepted = acceptedBinding(notebook);

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
    AcceptedBinding accepted = acceptedBinding(notebook);

    assertThrows(
        ConstraintViolationException.class,
        () -> controller.createNoteAtNotebookRoot(notebook, titleOnly("Taken")));
    assertBindingUnchanged(notebook, accepted);
  }
}
