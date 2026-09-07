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
 * Verifies {@code publishNotebookGitProposal} accepts the one rename shape the tree-shape gate
 * recognizes: an isolated same-parent, equal-content remove/add pair, retaining the original note's
 * identity under its filename-derived title. Cross-parent and content-changed pairs remain covered
 * as rejections in {@link NotebookGitProposalTreeShapeControllerTest}.
 */
class NotebookGitProposalRenameControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String TYPED_NOTE_CONTENT = "---\ntype: Note\n---\noriginal content";
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
