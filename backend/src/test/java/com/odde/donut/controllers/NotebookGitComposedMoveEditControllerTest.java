package com.odde.donut.controllers;

import static com.odde.donut.controllers.NotebookGitRenameScoringBodies.SUBSTANTIAL_ORIGINAL_BODY;
import static com.odde.donut.controllers.NotebookGitRenameScoringBodies.SUBSTANTIAL_UNRELATED_BODY;
import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.ConversationRepository;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.McqRepository;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verifies one publication of an exact unchanged-content note move followed by a later edit retains
 * the accepted note identity and learning data at the final path and content. Same-transition
 * changed-content move inference remains refused.
 */
class NotebookGitComposedMoveEditControllerTest extends NotebookGitControllerTestBase {

  private static final String ORIGINAL_CONTENT = "---\ntype: Note\n---\nORIGINAL_CONTENT";
  private static final String EDITED_CONTENT = "---\ntype: Note\n---\nEDITED_CONTENT";
  private static final String COMPANION_ORIGINAL = "---\ntype: Note\n---\nCompanion original";
  private static final String COMPANION_EDITED = "---\ntype: Note\n---\nCompanion edited";
  private static final String FOLDER_README = "---\ntype: Readme\n---\nDest folder.\n";

  @Autowired ConversationRepository conversationRepository;
  @Autowired FolderRepository folderRepository;
  @Autowired McqRepository mcqRepository;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  private Integer conversationId;

  @AfterEach
  void removeConversationBeforeCommittedUserCleanup() {
    if (conversationId != null) {
      inCommittedTransaction(
          transactionManager, () -> conversationRepository.deleteById(conversationId));
    }
  }

  @Test
  void publishesExactRenameThenEditRetainingNoteAndLearningIdentities() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note original =
        makeMe.aNote().notebook(notebook).title("Original").content(ORIGINAL_CONTENT).please();
    makeMe.aNote().notebook(notebook).title("Companion").content(COMPANION_ORIGINAL).please();
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(original.getId()).orElseThrow())
                    .nextRecallAt(makeMe.aTimestamp().of(5, 0).please())
                    .please());
    LearnedAssociations associations =
        inCommittedTransaction(
            transactionManager,
            () -> {
              Note reloaded = noteRepository.findById(original.getId()).orElseThrow();
              Mcq mcq = makeMe.anMcq().forNote(reloaded).please();
              Conversation conversation =
                  makeMe.aConversation().forANote(reloaded).from(currentUser.getUser()).please();
              conversationId = conversation.getId();
              return new LearnedAssociations(mcq.getId(), conversation.getId());
            });
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    ObjectId tip;
    byte[] proposalBytes;
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook));
      ObjectId afterRename =
          commitOnTopOf(
              repository,
              List.of(acceptedHead),
              List.of(
                  new NotebookGitProposalFile("Renamed.md", ORIGINAL_CONTENT),
                  new NotebookGitProposalFile("Companion.md", COMPANION_EDITED)),
              "Rename note and edit companion");
      tip =
          commitOnTopOf(
              repository,
              List.of(afterRename),
              List.of(
                  new NotebookGitProposalFile("Renamed.md", EDITED_CONTENT),
                  new NotebookGitProposalFile("Companion.md", COMPANION_EDITED)),
              "Edit renamed note");
      proposalBytes = bundleBytesForHead(repository, tip);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    assertThat(publishedHead, equalTo(tip.getName()));
    List<Note> storedNotes = noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(storedNotes, hasSize(2));
    Note renamed = noteRepository.findById(original.getId()).orElseThrow();
    assertThat(renamed.getTitle(), equalTo("Renamed"));
    assertThat(renamed.getContent(), equalTo(EDITED_CONTENT));
    assertThat(noteByTitle(storedNotes, "Companion").getContent(), equalTo(COMPANION_EDITED));

    MemoryTracker reloadedTracker = memoryTrackerRepository.findById(tracker.getId()).orElseThrow();
    assertThat(reloadedTracker.getNote().getId(), equalTo(original.getId()));
    assertThat(reloadedTracker.getNextRecallAt(), equalTo(tracker.getNextRecallAt()));
    assertThat(
        mcqRepository.findById(associations.mcqId()).orElseThrow().getNote().getId(),
        equalTo(original.getId()));
    assertThat(
        conversationRepository
            .findById(associations.conversationId())
            .orElseThrow()
            .getSubject()
            .getNote()
            .getId(),
        equalTo(original.getId()));
  }

  @Test
  void publishesExactMoveIntoEarlierAddedFolderThenEditRetainingIdentity() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note original =
        makeMe.aNote().notebook(notebook).title("note").content(ORIGINAL_CONTENT).please();
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(original.getId()).orElseThrow())
                    .nextRecallAt(makeMe.aTimestamp().of(7, 0).please())
                    .please());
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    ObjectId tip;
    byte[] proposalBytes;
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook));
      ObjectId afterAddFolder =
          commitOnTopOf(
              repository,
              List.of(acceptedHead),
              List.of(
                  new NotebookGitProposalFile("note.md", ORIGINAL_CONTENT),
                  new NotebookGitProposalFile("Dest/README.md", FOLDER_README)),
              "Add dest folder");
      ObjectId afterMove =
          commitOnTopOf(
              repository,
              List.of(afterAddFolder),
              List.of(
                  new NotebookGitProposalFile("Dest/README.md", FOLDER_README),
                  new NotebookGitProposalFile("Dest/note.md", ORIGINAL_CONTENT)),
              "Move note into dest");
      tip =
          commitOnTopOf(
              repository,
              List.of(afterMove),
              List.of(
                  new NotebookGitProposalFile("Dest/README.md", FOLDER_README),
                  new NotebookGitProposalFile("Dest/note.md", EDITED_CONTENT)),
              "Edit moved note");
      proposalBytes = bundleBytesForHead(repository, tip);
    }

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Note moved = noteRepository.findById(original.getId()).orElseThrow();
    assertThat(moved.getTitle(), equalTo("note"));
    assertThat(moved.getContent(), equalTo(EDITED_CONTENT));
    assertThat(moved.getFolder().getId(), equalTo(folderNamed(notebook, "Dest").getId()));
    MemoryTracker reloadedTracker = memoryTrackerRepository.findById(tracker.getId()).orElseThrow();
    assertThat(reloadedTracker.getNote().getId(), equalTo(original.getId()));
  }

  @Test
  void publishesMoveIntoNewlyCreatedFolderAlongsideAnotherNoteEdit() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note moved = makeMe.aNote().notebook(notebook).title("note").content(ORIGINAL_CONTENT).please();
    Note companion =
        makeMe.aNote().notebook(notebook).title("other").content(COMPANION_ORIGINAL).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Alpha/note.md", ORIGINAL_CONTENT),
                new NotebookGitProposalFile("other.md", COMPANION_EDITED)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Note reloadedMoved = noteRepository.findById(moved.getId()).orElseThrow();
    assertThat(reloadedMoved.getTitle(), equalTo("note"));
    assertThat(reloadedMoved.getContent(), equalTo(ORIGINAL_CONTENT));
    assertThat(reloadedMoved.getFolder().getId(), equalTo(folderNamed(notebook, "Alpha").getId()));
    assertThat(
        noteRepository.findById(companion.getId()).orElseThrow().getContent(),
        equalTo(COMPANION_EDITED));
  }

  @Test
  void stillRefusesSameTransitionChangedContentMove() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("Original").content(SUBSTANTIAL_ORIGINAL_BODY).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(new NotebookGitProposalFile("Renamed.md", SUBSTANTIAL_UNRELATED_BODY)));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposalBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("identity correspondence is uncertain"));
  }

  private Folder folderNamed(Notebook notebook, String name) {
    return folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
        .filter(folder -> name.equals(folder.getName()))
        .findFirst()
        .orElseThrow();
  }

  private static Note noteByTitle(List<Note> notes, String title) {
    return notes.stream().filter(note -> title.equals(note.getTitle())).findFirst().orElseThrow();
  }

  private record LearnedAssociations(Integer mcqId, Integer conversationId) {}
}
