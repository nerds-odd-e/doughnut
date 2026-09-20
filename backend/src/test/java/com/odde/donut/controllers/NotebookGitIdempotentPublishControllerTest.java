package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Timestamp;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookGitIdempotentPublishControllerTest extends NotebookGitWebContentControllerTestBase {

  private static final String ORIGINAL_CONTENT = "---\ntype: Note\n---\nOriginal authored bytes.\n";
  private static final String RETAINED_EDITED_CONTENT =
      "---\ntype: Note\n---\nEdited authored bytes.\n";
  private static final String ADDED_CONTENT = "---\ntype: Note\n---\nNewly added authored bytes.\n";

  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @Test
  void idempotentPublishReturnsUnchangedHeadAndHistory() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    String expectedHead = binding.getAcceptedGitObjectId();
    byte[] currentBundle = binding.getBundleBytes();

    String publishedHead =
        controller.publishNotebookGitProposal(notebook.getId(), expectedHead, currentBundle);

    assertThat(publishedHead, equalTo(expectedHead));
    NotebookGitBinding after =
        inCommittedTransaction(
            transactionManager,
            () -> notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow());
    assertThat(after.getAcceptedGitObjectId(), equalTo(expectedHead));
    assertThat(after.getBundleBytes(), equalTo(currentBundle));
  }

  @Test
  void rejectedPublishLeavesAcceptedHistoryUnchanged() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    String acceptedHead = binding.getAcceptedGitObjectId();
    byte[] currentBundle = binding.getBundleBytes();
    currentUser.setUser(createFixtureUser());

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.publishNotebookGitProposal(notebook.getId(), acceptedHead, currentBundle));

    NotebookGitBinding after =
        inCommittedTransaction(
            transactionManager,
            () -> notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow());
    assertThat(after.getAcceptedGitObjectId(), equalTo(acceptedHead));
    assertThat(after.getBundleBytes(), equalTo(currentBundle));
  }

  @Test
  void retriesAnAlreadyAcceptedMultiCommitRangeWithoutRepeatingCreationsOrRemovals()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note deleted =
        makeMe.aNote().notebook(notebook).title("Deleted").content(ORIGINAL_CONTENT).please();
    makeMe.aNote().notebook(notebook).title("Retained").content(ORIGINAL_CONTENT).please();
    MemoryTracker deletedTracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(deleted.getId()).orElseThrow())
                    .difficulty(7f)
                    .please());
    inCommittedTransaction(
        transactionManager,
        () -> {
          makeMe
              .aRecallPrompt()
              .forMemoryTracker(
                  memoryTrackerRepository.findById(deletedTracker.getId()).orElseThrow())
              .withMcqForNote(noteRepository.findById(deleted.getId()).orElseThrow())
              .please();
          makeMe.anImage().forNote(noteRepository.findById(deleted.getId()).orElseThrow()).please();
          makeMe
              .aConversation()
              .forANote(noteRepository.findById(deleted.getId()).orElseThrow())
              .please();
        });
    NotebookGitBinding initialBinding = snapshotCurrentPortableTree(notebook);
    String initialHead = initialBinding.getAcceptedGitObjectId();
    ObjectId acceptedHead = ObjectId.fromString(initialHead);
    ObjectId afterDeleteAndEdit;
    ObjectId tip;
    byte[] proposalBytes;
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.fetchHead(repository, initialBinding.getBundleBytes());
      afterDeleteAndEdit =
          commitOnTopOf(
              repository,
              List.of(acceptedHead),
              List.of(new NotebookGitProposalFile("Retained.md", RETAINED_EDITED_CONTENT)),
              "Delete learned note and edit retained");
      tip =
          commitOnTopOf(
              repository,
              List.of(afterDeleteAndEdit),
              List.of(
                  new NotebookGitProposalFile("Retained.md", RETAINED_EDITED_CONTENT),
                  new NotebookGitProposalFile("Added.md", ADDED_CONTENT)),
              "Add unrelated note");
      proposalBytes = bundleBytesForHead(repository, tip);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(notebook.getId(), initialHead, proposalBytes);
    PublicationState stateAfterPublication = publicationState(notebook, deleted);
    assertThat(publishedHead, equalTo(tip.getName()));
    assertThat(stateAfterPublication.notes(), hasSize(2));
    assertThat(stateAfterPublication.deletedPresent(), equalTo(false));
    assertThat(stateAfterPublication.dependentCounts(), equalTo(DependentCounts.allAbsent()));

    byte[] downloadedAfterPublication =
        controller
            .downloadNotebookGitBundle(notebookRepository.findById(notebook.getId()).orElseThrow())
            .getBody();
    try (InMemoryRepository accepted = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk revWalk = new RevWalk(accepted)) {
      ObjectId downloadedHead = GitBundleTestReader.fetchHead(accepted, downloadedAfterPublication);
      assertThat(downloadedHead, equalTo(tip));
      RevCommit tipCommit = revWalk.parseCommit(downloadedHead);
      assertThat(tipCommit.getParentCount(), equalTo(1));
      assertThat(tipCommit.getParent(0).getId(), equalTo(afterDeleteAndEdit));
      RevCommit middleCommit = revWalk.parseCommit(tipCommit.getParent(0));
      assertThat(middleCommit.getParentCount(), equalTo(1));
      assertThat(middleCommit.getParent(0).getId(), equalTo(acceptedHead));
      assertThat(
          GitBundleTestReader.pathsIn(accepted, downloadedHead),
          containsInAnyOrder("Retained.md", "Added.md"));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(accepted, downloadedHead, "Retained.md"),
          equalTo(RETAINED_EDITED_CONTENT));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(accepted, downloadedHead, "Added.md"),
          equalTo(ADDED_CONTENT));
    }

    testabilitySettings.timeTravelTo(Timestamp.valueOf("2020-06-01 00:00:00"));
    String retriedHead =
        controller.publishNotebookGitProposal(notebook.getId(), initialHead, proposalBytes);

    assertThat(retriedHead, equalTo(publishedHead));
    PublicationState stateAfterRetry = publicationState(notebook, deleted);
    assertThat(stateAfterRetry.acceptedHead(), equalTo(stateAfterPublication.acceptedHead()));
    assertThat(
        stateAfterRetry.bindingUpdatedAt(), equalTo(stateAfterPublication.bindingUpdatedAt()));
    assertThat(stateAfterRetry.notes(), equalTo(stateAfterPublication.notes()));
    assertThat(stateAfterRetry.deletedPresent(), equalTo(stateAfterPublication.deletedPresent()));
    assertThat(stateAfterRetry.dependentCounts(), equalTo(stateAfterPublication.dependentCounts()));
  }

  private PublicationState publicationState(Notebook notebook, Note deleted) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          NotebookGitBinding binding =
              notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
          List<Note> notes = noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId());
          return new PublicationState(
              binding.getAcceptedGitObjectId(),
              binding.getUpdatedAt(),
              notes.stream()
                  .map(
                      note ->
                          new PublishedNoteState(
                              note.getId(),
                              note.getTitle(),
                              note.getContent(),
                              note.getCreatedAt(),
                              note.getUpdatedAt()))
                  .toList(),
              noteRepository.findById(deleted.getId()).isPresent(),
              dependentCounts(deleted));
        });
  }

  private record PublicationState(
      String acceptedHead,
      Timestamp bindingUpdatedAt,
      List<PublishedNoteState> notes,
      boolean deletedPresent,
      DependentCounts dependentCounts) {}

  private record PublishedNoteState(
      Integer noteId,
      String noteTitle,
      String noteContent,
      Timestamp noteCreatedAt,
      Timestamp noteUpdatedAt) {}
}
