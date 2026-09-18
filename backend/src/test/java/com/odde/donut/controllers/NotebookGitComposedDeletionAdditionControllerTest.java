package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.NoteRecallInfo;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verifies one publication that deletes a learned note, edits another, and later adds a different
 * note applies permanent removal once for the deleted identity while retaining surviving learning
 * data and authored referrers. Same-transition unmatched remove/add pairs remain refused.
 */
class NotebookGitComposedDeletionAdditionControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String ORIGINAL_CONTENT = "---\ntype: Note\n---\nOriginal authored bytes.\n";
  private static final String EDITED_CONTENT = "---\ntype: Note\n---\nEdited authored bytes.\n";
  private static final String ADDED_CONTENT = "---\ntype: Note\n---\nNewly added authored bytes.\n";
  private static final String REFERRER_CONTENT =
      "---\n" + "type: Note\n" + "example of: \"[[Deleted]]\"\n" + "---\n" + "Body [[Deleted]]\n";

  @Autowired NoteController noteController;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @Test
  void publishesDeletionThenEditThenUnrelatedAdditionWithPermanentRemovalClosure()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note deleted =
        makeMe.aNote().notebook(notebook).title("Deleted").content(ORIGINAL_CONTENT).please();
    Note retained =
        makeMe.aNote().notebook(notebook).title("Retained").content(ORIGINAL_CONTENT).please();
    Note referrer =
        makeMe.aNote().notebook(notebook).title("Referrer").content(REFERRER_CONTENT).please();
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
          makeMe.authorReferencingContent(
              noteRepository.findById(referrer.getId()).orElseThrow(), REFERRER_CONTENT);
        });
    MemoryTracker retainedTracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(retained.getId()).orElseThrow())
                    .difficulty(5f)
                    .please());
    List<WikiLink.Resolution> resolutionsBeforePublish =
        deletedResolutions(
            noteController.showNote(noteRepository.findById(referrer.getId()).orElseThrow()));
    assertThat(resolutionsBeforePublish, not(empty()));
    assertThat(resolutionsBeforePublish, everyItem(equalTo(WikiLink.Resolution.RESOLVED)));

    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    ObjectId tip;
    byte[] proposalBytes;
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.fetchHead(repository, binding.getBundleBytes());
      ObjectId afterDeleteAndEdit =
          commitOnTopOf(
              repository,
              List.of(acceptedHead),
              List.of(
                  new NotebookGitProposalFile("Retained.md", EDITED_CONTENT),
                  new NotebookGitProposalFile("Referrer.md", REFERRER_CONTENT)),
              "Delete learned note and edit retained");
      tip =
          commitOnTopOf(
              repository,
              List.of(afterDeleteAndEdit),
              List.of(
                  new NotebookGitProposalFile("Retained.md", EDITED_CONTENT),
                  new NotebookGitProposalFile("Referrer.md", REFERRER_CONTENT),
                  new NotebookGitProposalFile("Added.md", ADDED_CONTENT)),
              "Add unrelated note");
      proposalBytes = bundleBytesForHead(repository, tip);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    assertThat(publishedHead, equalTo(tip.getName()));
    List<Note> storedNotes = noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(storedNotes, hasSize(3));
    assertThat(noteRepository.findById(deleted.getId()).isPresent(), equalTo(false));
    assertThat(
        inCommittedTransaction(transactionManager, () -> dependentCounts(deleted)),
        equalTo(DependentCounts.allAbsent()));
    assertThat(
        memoryTrackerRepository.findById(deletedTracker.getId()).isPresent(), equalTo(false));

    Note reloadedRetained = noteRepository.findById(retained.getId()).orElseThrow();
    assertThat(reloadedRetained.getContent(), equalTo(EDITED_CONTENT));
    NoteRecallInfo retainedRecall = noteController.getNoteInfo(reloadedRetained);
    assertThat(retainedRecall.getMemoryTrackers(), hasSize(1));
    assertThat(
        retainedRecall.getMemoryTrackers().getFirst().getId(), equalTo(retainedTracker.getId()));

    Note added = noteByTitle(storedNotes, "Added");
    assertThat(added.getContent(), equalTo(ADDED_CONTENT));
    assertThat(added.getId(), not(equalTo(deleted.getId())));

    NoteRealm shownReferrer =
        noteController.showNote(noteRepository.findById(referrer.getId()).orElseThrow());
    assertThat(shownReferrer.getNote().getContent(), equalTo(REFERRER_CONTENT));
    assertThat(deletedResolutions(shownReferrer), empty());
  }

  @Test
  void stillRefusesSameTransitionUnmatchedRemovalAndAddition() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("First").content(ORIGINAL_CONTENT).please();
    makeMe.aNote().notebook(notebook).title("Second").content(ORIGINAL_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            binding.getAcceptedGitObjectId(),
            proposalBundleBytes(
                binding,
                List.of(
                    new NotebookGitProposalFile("Second.md", ORIGINAL_CONTENT),
                    new NotebookGitProposalFile("Added.md", ADDED_CONTENT))),
            HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("identity correspondence is uncertain"));
  }

  private static Note noteByTitle(List<Note> notes, String title) {
    return notes.stream().filter(note -> title.equals(note.getTitle())).findFirst().orElseThrow();
  }

  private static List<WikiLink.Resolution> deletedResolutions(NoteRealm shown) {
    return shown.getWikiLinks().stream()
        .filter(link -> "Deleted".equals(link.getTarget()))
        .map(WikiLink::getResolution)
        .toList();
  }
}
