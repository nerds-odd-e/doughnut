package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verifies publication of an in-place folder Readme edit, alone or with a companion same-path body
 * edit to an existing learned note. Folder identity is preserved; learning associations survive the
 * companion edit; invalid typed Markdown leaves accepted content unchanged.
 */
class NotebookGitProposalFolderReadmeEditControllerTest
    extends NotebookGitWebContentControllerTestBase {

  private static final String TYPED_NOTE_CONTENT = "---\ntype: Note\n---\noriginal content";
  private static final String EDITED_NOTE_CONTENT = "---\ntype: Note\n---\nchanged body";
  private static final String EDITED_FOLDER_README =
      "---\ntype: Readme\nauthor: owner\n---\nchanged folder landing\n";

  @Test
  void acceptsAValidFolderReadmeEditAsTheSoleFolderDescriptionWithoutMutatingFolderIdentity()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder folder =
        makeMe
            .aFolder()
            .notebook(notebook)
            .name("Recipes")
            .readmeContent("original readme")
            .please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(new NotebookGitProposalFile("Recipes/README.md", EDITED_FOLDER_README)));

    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Folder reloaded = entityManager.find(Folder.class, folder.getId());
    assertThat(reloaded.getId(), equalTo(folder.getId()));
    assertThat(reloaded.getReadmeContent(), equalTo(EDITED_FOLDER_README));
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));
    assertThat(noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()), hasSize(0));

    ResponseEntity<byte[]> downloaded =
        controller.downloadNotebookGitBundle(
            notebookRepository.findById(notebook.getId()).orElseThrow());
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(
              readBack, downloadedCommit.head(), "Recipes/README.md"),
          equalTo(EDITED_FOLDER_README));
    }
  }

  @Test
  void acceptsAValidFolderReadmeEditAlongsideACompanionExistingNoteEditRetainingLearning()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder folder =
        makeMe
            .aFolder()
            .notebook(notebook)
            .name("Recipes")
            .readmeContent("original readme")
            .please();
    Note note = makeMe.aNote().folder(folder).title("Pasta").content(TYPED_NOTE_CONTENT).please();
    MemoryTracker tracker = learnedTracker(note, 7f, 2);
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Recipes/README.md", EDITED_FOLDER_README),
                new NotebookGitProposalFile("Recipes/Pasta.md", EDITED_NOTE_CONTENT)));

    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Folder reloadedFolder = entityManager.find(Folder.class, folder.getId());
    assertThat(reloadedFolder.getId(), equalTo(folder.getId()));
    assertThat(reloadedFolder.getReadmeContent(), equalTo(EDITED_FOLDER_README));
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));
    assertShownContentAndRetainedLearning(note, tracker, EDITED_NOTE_CONTENT);
    assertThat(
        "recall log history survives the companion note edit",
        countRecallLogsByTrackerId(tracker.getId()),
        equalTo(2L));
  }

  @Test
  void rejectsAnInvalidFolderReadmeEditWithoutMutatingAcceptedContent() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder folder =
        makeMe
            .aFolder()
            .notebook(notebook)
            .name("Recipes")
            .readmeContent("original readme")
            .please();
    Note note = makeMe.aNote().folder(folder).title("Pasta").content(TYPED_NOTE_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Recipes/README.md", "changed readme, no frontmatter"),
                new NotebookGitProposalFile("Recipes/Pasta.md", EDITED_NOTE_CONTENT)));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposalBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("Recipes/README.md"));
    assertThat(
        entityManager.find(Folder.class, folder.getId()).getReadmeContent(),
        equalTo("original readme"));
    assertThat(
        noteRepository.findById(note.getId()).orElseThrow().getContent(),
        equalTo(TYPED_NOTE_CONTENT));
  }
}
