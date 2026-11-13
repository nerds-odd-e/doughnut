package com.odde.donut.controllers;

import static com.odde.donut.controllers.NotebookGitRenameScoringBodies.SUBSTANTIAL_ORIGINAL_BODY;
import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/**
 * Verifies {@code publishNotebookGitProposal} accepts identity-preserving ordinary-note moves,
 * including compatible companion changes. Private associations are covered in {@link
 * NotebookGitProposalRenamePrivateAssociationControllerTest}. Referrer side-effects are covered in
 * {@link NotebookGitProposalRenameReferrerControllerTest}. Folder-subtree relocation is covered in
 * {@link NotebookGitProposalRelocationControllerTest}. Content-changed mixed pairs remain covered
 * as rejections in {@link NotebookGitProposalTreeShapeControllerTest}.
 */
class NotebookGitProposalRenameControllerTest extends NotebookGitControllerTestBase {
  private static final String TYPED_NOTE_CONTENT = "---\ntype: Note\n---\noriginal content";
  private static final String OTHER_NOTE_CONTENT = "---\ntype: Note\n---\nother content";
  private static final String EDITED_NOTE_CONTENT = "---\ntype: Note\n---\nedited content";
  private static final String FOLDER_ANCHOR_CONTENT = "---\ntype: Note\n---\nfolder anchor";
  // SMALL_EDIT_BODY scores well above 60 against SUBSTANTIAL_ORIGINAL_BODY.
  private static final String SMALL_EDIT_BODY =
      "---\ntype: Note\n---\n"
          + "The quick brown fox jumps over the lazy dog near the riverbank.\n"
          + "She decided to read the ancient manuscript that described the valley.\n"
          + "Mountains rose in the distance, their peaks covered with fresh snow.\n"
          + "A small village nestled between them kept its traditions alive for generations.\n"
          + "Travelers came each spring to trade cloth and spices at the market.\n"
          + "Children played near the fountain while elders discussed the harvest.\n"
          + "The librarian organized every scroll by region and by season, carefully.\n";

  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @Test
  void acceptsASameParentRenameRetainingTheOriginalNoteIdentity() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("note").content(TYPED_NOTE_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("renamed.md", TYPED_NOTE_CONTENT)));

    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    List<Note> notes = noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(notes, hasSize(1));
    Note renamed = notes.getFirst();
    assertThat(renamed.getId(), equalTo(note.getId()));
    assertThat(renamed.getTitle(), equalTo("renamed"));
    assertThat(renamed.getContent(), equalTo(TYPED_NOTE_CONTENT));
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));

    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
      assertThat(downloadedCommit.parent().getName(), equalTo(binding.getAcceptedGitObjectId()));
    }
  }

  @Test
  void acceptsASameParentRenameInsideANestedFolder() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder folder = makeMe.aFolder().notebook(notebook).name("Folder").please();
    Note note = makeMe.aNote().folder(folder).title("note").content(TYPED_NOTE_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("Folder/renamed.md", TYPED_NOTE_CONTENT)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Note renamed = noteRepository.findById(note.getId()).orElseThrow();
    assertThat(renamed.getTitle(), equalTo("renamed"));
    assertThat(renamed.getFolder().getId(), equalTo(folder.getId()));
    assertThat(renamed.getContent(), equalTo(TYPED_NOTE_CONTENT));
  }

  @Test
  void acceptsAMoveIntoAnExistingFolderWithEditedContentRetainingNoteAndLearningIdentities()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder destination = makeMe.aFolder().notebook(notebook).name("Dest").please();
    Note note =
        makeMe.aNote().notebook(notebook).title("note").content(SUBSTANTIAL_ORIGINAL_BODY).please();
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(note.getId()).orElseThrow())
                    .nextRecallAt(makeMe.aTimestamp().of(7, 0).please())
                    .please());
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("Dest/note.md", SMALL_EDIT_BODY)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Note moved = noteRepository.findById(note.getId()).orElseThrow();
    assertThat(moved.getId(), equalTo(note.getId()));
    assertThat(moved.getTitle(), equalTo("note"));
    assertThat(moved.getFolder().getId(), equalTo(destination.getId()));
    assertThat(moved.getContent(), equalTo(SMALL_EDIT_BODY));
    MemoryTracker reloadedTracker = memoryTrackerRepository.findById(tracker.getId()).orElseThrow();
    assertThat(reloadedTracker.getNote().getId(), equalTo(note.getId()));
    assertThat(reloadedTracker.getNextRecallAt(), equalTo(tracker.getNextRecallAt()));
  }

  @Test
  void acceptsAUniqueMoveWithACompanionEditWhileIgnoringAnUnchangedEqualBlob() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note moved = makeMe.aNote().notebook(notebook).title("A").content(TYPED_NOTE_CONTENT).please();
    Note edited = makeMe.aNote().notebook(notebook).title("B").content(OTHER_NOTE_CONTENT).please();
    Note unchanged =
        makeMe.aNote().notebook(notebook).title("Unchanged").content(TYPED_NOTE_CONTENT).please();
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(moved.getId()).orElseThrow())
                    .removedFromTracking()
                    .please());
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);

    controller.publishNotebookGitProposal(
        notebook.getId(),
        binding.getAcceptedGitObjectId(),
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("D.md", TYPED_NOTE_CONTENT),
                new NotebookGitProposalFile("B.md", EDITED_NOTE_CONTENT),
                new NotebookGitProposalFile("Unchanged.md", TYPED_NOTE_CONTENT))));

    assertThat(noteRepository.findById(moved.getId()).orElseThrow().getTitle(), equalTo("D"));
    assertThat(
        noteRepository.findById(edited.getId()).orElseThrow().getContent(),
        equalTo(EDITED_NOTE_CONTENT));
    assertThat(
        noteRepository.findById(unchanged.getId()).orElseThrow().getTitle(), equalTo("Unchanged"));
    assertThat(
        memoryTrackerRepository.findById(tracker.getId()).orElseThrow().getNote().getId(),
        equalTo(moved.getId()));
  }

  @Test
  void acceptsSeveralUniqueMovesRegardlessOfPathOrdering() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder destination = makeMe.aFolder().notebook(notebook).name("Folder").please();
    makeMe.aNote().folder(destination).title("Anchor").content(FOLDER_ANCHOR_CONTENT).please();
    Note first =
        makeMe.aNote().notebook(notebook).title("ZSource").content(TYPED_NOTE_CONTENT).please();
    Note second =
        makeMe.aNote().notebook(notebook).title("ASource").content(OTHER_NOTE_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);

    controller.publishNotebookGitProposal(
        notebook.getId(),
        binding.getAcceptedGitObjectId(),
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("ZDestination.md", OTHER_NOTE_CONTENT),
                new NotebookGitProposalFile("Folder/ADestination.md", TYPED_NOTE_CONTENT),
                new NotebookGitProposalFile("Folder/Anchor.md", FOLDER_ANCHOR_CONTENT))));

    Note movedFirst = noteRepository.findById(first.getId()).orElseThrow();
    assertThat(movedFirst.getTitle(), equalTo("ADestination"));
    assertThat(movedFirst.getFolder().getId(), equalTo(destination.getId()));
    assertThat(
        noteRepository.findById(second.getId()).orElseThrow().getTitle(), equalTo("ZDestination"));
  }

  @Test
  void acceptsAUniqueMoveWithAnAdditionOnlyCompanion() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note moved = makeMe.aNote().notebook(notebook).title("A").content(TYPED_NOTE_CONTENT).please();
    makeMe.aNote().notebook(notebook).title("Keeper").content(OTHER_NOTE_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);

    controller.publishNotebookGitProposal(
        notebook.getId(),
        binding.getAcceptedGitObjectId(),
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Moved.md", TYPED_NOTE_CONTENT),
                new NotebookGitProposalFile("Keeper.md", OTHER_NOTE_CONTENT),
                new NotebookGitProposalFile("Added.md", EDITED_NOTE_CONTENT))));

    assertThat(noteRepository.findById(moved.getId()).orElseThrow().getTitle(), equalTo("Moved"));
    assertThat(noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId()), hasSize(3));
  }

  @Test
  void acceptsAUniqueMoveWithADeletionOnlyCompanion() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note moved = makeMe.aNote().notebook(notebook).title("A").content(TYPED_NOTE_CONTENT).please();
    Note deleted =
        makeMe.aNote().notebook(notebook).title("Delete").content(OTHER_NOTE_CONTENT).please();
    makeMe.aNote().notebook(notebook).title("Keeper").content(EDITED_NOTE_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);

    controller.publishNotebookGitProposal(
        notebook.getId(),
        binding.getAcceptedGitObjectId(),
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Moved.md", TYPED_NOTE_CONTENT),
                new NotebookGitProposalFile("Keeper.md", EDITED_NOTE_CONTENT))));

    assertThat(noteRepository.findById(moved.getId()).orElseThrow().getTitle(), equalTo("Moved"));
    assertThat(noteRepository.findById(deleted.getId()).isPresent(), equalTo(false));
  }
}
