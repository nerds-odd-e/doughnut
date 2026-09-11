package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.ConversationRepository;
import com.odde.donut.entities.repositories.McqRepository;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/**
 * Verifies {@code publishNotebookGitProposal} accepts identity-preserving ordinary-note moves,
 * including compatible companion changes. Folder-subtree relocation is covered in {@link
 * NotebookGitProposalRelocationControllerTest}. Content-changed mixed pairs remain covered as
 * rejections in {@link NotebookGitProposalTreeShapeControllerTest}.
 */
class NotebookGitProposalRenameControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String TYPED_NOTE_CONTENT = "---\ntype: Note\n---\noriginal content";
  private static final String OTHER_NOTE_CONTENT = "---\ntype: Note\n---\nother content";
  private static final String EDITED_NOTE_CONTENT = "---\ntype: Note\n---\nedited content";
  private static final String FOLDER_ANCHOR_CONTENT = "---\ntype: Note\n---\nfolder anchor";
  private static final String MATCHING_CONTENT = "---\ntype: Note\n---\nmatching learned content";
  private static final String TARGET_CONTENT = "---\ntype: Note\n---\nOriginal authored bytes.\n";
  private static final String REFERRER_CONTENT =
      "---\n" + "type: Note\n" + "example of: \"[[Target]]\"\n" + "---\n" + "Body [[Target]]\n";

  @Autowired ConversationRepository conversationRepository;
  @Autowired McqRepository mcqRepository;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;
  @Autowired NoteController noteController;

  private Integer conversationId;

  @AfterEach
  void removeConversationBeforeCommittedUserCleanup() {
    if (conversationId != null) {
      inCommittedTransaction(
          transactionManager, () -> conversationRepository.deleteById(conversationId));
    }
  }

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

    List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
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
    assertThat(noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()), hasSize(3));
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
    assertThat(
        noteRepository.findById(deleted.getId()).orElseThrow().getDeletedAt(), not(equalTo(null)));
  }

  @Test
  void preservesPrivateAssociationsOfTheRenamedNoteAndLeavesTheOtherIdenticalTextNoteUntouched()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note renamedNote =
        makeMe.aNote().notebook(notebook).title("Original").content(MATCHING_CONTENT).please();
    Note untouchedNote =
        makeMe.aNote().notebook(notebook).title("Untouched").content(MATCHING_CONTENT).please();

    MemoryTracker trackerForRenamed =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(renamedNote.getId()).orElseThrow())
                    .removedFromTracking()
                    .nextRecallAt(makeMe.aTimestamp().of(5, 0).please())
                    .please());
    MemoryTracker trackerForUntouched =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(untouchedNote.getId()).orElseThrow())
                    .nextRecallAt(makeMe.aTimestamp().of(10, 0).please())
                    .please());
    RenamedNoteAssociations associations =
        inCommittedTransaction(
            transactionManager,
            () -> {
              Note reloadedRenamed = noteRepository.findById(renamedNote.getId()).orElseThrow();
              Mcq mcq = makeMe.anMcq().forNote(reloadedRenamed).please();
              Conversation conversation =
                  makeMe
                      .aConversation()
                      .forANote(reloadedRenamed)
                      .from(currentUser.getUser())
                      .please();
              conversationId = conversation.getId();
              return new RenamedNoteAssociations(mcq.getId(), conversation.getId());
            });

    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("renamed.md", MATCHING_CONTENT),
                new NotebookGitProposalFile("Untouched.md", MATCHING_CONTENT)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Note reloadedRenamed = noteRepository.findById(renamedNote.getId()).orElseThrow();
    assertThat(reloadedRenamed.getId(), equalTo(renamedNote.getId()));
    assertThat(reloadedRenamed.getTitle(), equalTo("renamed"));
    Note reloadedUntouched = noteRepository.findById(untouchedNote.getId()).orElseThrow();
    assertThat(reloadedUntouched.getId(), equalTo(untouchedNote.getId()));
    assertThat(reloadedUntouched.getTitle(), equalTo("Untouched"));

    MemoryTracker reloadedTrackerForRenamed =
        memoryTrackerRepository.findById(trackerForRenamed.getId()).orElseThrow();
    assertThat(reloadedTrackerForRenamed.getNote().getId(), equalTo(renamedNote.getId()));
    assertThat(
        reloadedTrackerForRenamed.getRemovedFromTracking(),
        equalTo(trackerForRenamed.getRemovedFromTracking()));
    assertThat(
        reloadedTrackerForRenamed.getNextRecallAt(), equalTo(trackerForRenamed.getNextRecallAt()));

    MemoryTracker reloadedTrackerForUntouched =
        memoryTrackerRepository.findById(trackerForUntouched.getId()).orElseThrow();
    assertThat(reloadedTrackerForUntouched.getNote().getId(), equalTo(untouchedNote.getId()));
    assertThat(
        reloadedTrackerForUntouched.getRemovedFromTracking(),
        equalTo(trackerForUntouched.getRemovedFromTracking()));
    assertThat(
        reloadedTrackerForUntouched.getNextRecallAt(),
        equalTo(trackerForUntouched.getNextRecallAt()));

    Mcq mcq = mcqRepository.findById(associations.mcqId()).orElseThrow();
    assertThat(mcq.getNote().getId(), equalTo(renamedNote.getId()));
    Conversation conversation =
        conversationRepository.findById(associations.conversationId()).orElseThrow();
    assertThat(conversation.getSubject().getNote().getId(), equalTo(renamedNote.getId()));
  }

  @Test
  void leavesReferringBodyAndPropertyLinksAuthoredWhenPublishingTheTargetsRename()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note target =
        makeMe.aNote().notebook(notebook).title("Target").content(TARGET_CONTENT).please();
    Note referrer =
        makeMe.aNote().notebook(notebook).title("Referrer").content(REFERRER_CONTENT).please();
    inCommittedTransaction(
        transactionManager,
        () ->
            makeMe.authorReferencingContent(
                noteRepository.findById(referrer.getId()).orElseThrow(), REFERRER_CONTENT));
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    List<WikiLink.Resolution> resolutionsBeforePublish =
        targetResolutions(
            noteController.showNote(noteRepository.findById(referrer.getId()).orElseThrow()));
    assertThat(resolutionsBeforePublish, not(empty()));
    assertThat(resolutionsBeforePublish, everyItem(equalTo(WikiLink.Resolution.RESOLVED)));
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Target Renamed.md", TARGET_CONTENT),
                new NotebookGitProposalFile("Referrer.md", REFERRER_CONTENT)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    NoteRealm shown =
        noteController.showNote(noteRepository.findById(referrer.getId()).orElseThrow());
    assertThat(shown.getNote().getContent(), equalTo(REFERRER_CONTENT));
    assertThat(targetResolutions(shown), empty());
    Note renamedTarget = noteRepository.findById(target.getId()).orElseThrow();
    assertThat(renamedTarget.getTitle(), equalTo("Target Renamed"));
  }

  private static List<WikiLink.Resolution> targetResolutions(NoteRealm shown) {
    return shown.getWikiLinks().stream()
        .filter(link -> "Target".equals(link.getTarget()))
        .map(WikiLink::getResolution)
        .toList();
  }

  private record RenamedNoteAssociations(Integer mcqId, Integer conversationId) {}
}
