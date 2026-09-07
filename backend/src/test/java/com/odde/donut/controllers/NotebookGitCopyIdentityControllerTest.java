package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.ConversationRepository;
import com.odde.donut.entities.repositories.McqRepository;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Verifies that copied Portable content receives a fresh Donut identity. */
class NotebookGitCopyIdentityControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String COPIED_CONTENT =
      "---\ntype: Note\ntopic: inertia\n---\nA body copied without changes.\n";

  @Autowired NoteController noteController;
  @Autowired ConversationMessageController conversationMessageController;
  @Autowired ConversationRepository conversationRepository;
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
  void publishesCopiedContentWithAFreshIdentityAndNoPrivateAssociations() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note original =
        makeMe.aNote().notebook(notebook).title("Original").content(COPIED_CONTENT).please();
    OriginalAssociations associations = commitPrivateAssociations(original);
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposal =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Original.md", COPIED_CONTENT),
                new NotebookGitProposalFile("Copy.md", COPIED_CONTENT)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposal);

    List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(notes, hasSize(2));
    Note reloadedOriginal = noteRepository.findById(original.getId()).orElseThrow();
    Note copy =
        notes.stream().filter(note -> note.getTitle().equals("Copy")).findFirst().orElseThrow();
    NoteRealm originalView = noteController.showNote(reloadedOriginal);
    NoteRealm copyView = noteController.showNote(copy);
    assertThat(originalView.getId(), equalTo(original.getId()));
    assertThat(copyView.getId(), not(equalTo(originalView.getId())));
    assertThat(originalView.getNote().getContent(), equalTo(COPIED_CONTENT));
    assertThat(copyView.getNote().getContent(), equalTo(COPIED_CONTENT));

    assertThat(
        noteController.getNoteInfo(reloadedOriginal).getMemoryTrackers().stream()
            .map(MemoryTracker::getId)
            .toList(),
        contains(associations.trackerId()));
    assertThat(mcqIdsForNote(reloadedOriginal), contains(associations.mcqId()));
    assertThat(
        conversationMessageController.getConversationsAboutNote(reloadedOriginal).stream()
            .map(Conversation::getId)
            .toList(),
        contains(associations.conversationId()));
    assertHasNoPrivateAssociations(copy);
  }

  @Test
  void publishesALaterSameContentAdditionWithAFreshIdentityAfterAcceptedDeletion()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note original =
        makeMe.aNote().notebook(notebook).title("Original").content(COPIED_CONTENT).please();
    OriginalAssociations associations = commitPrivateAssociations(original);
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] deletionProposal = proposalBundleBytes(binding, List.of());

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), deletionProposal);

    NotebookGitBinding afterDeletion =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    byte[] additionProposal =
        proposalBundleBytes(
            afterDeletion, List.of(new NotebookGitProposalFile("Copy.md", COPIED_CONTENT)));

    controller.publishNotebookGitProposal(
        notebook.getId(), afterDeletion.getAcceptedGitObjectId(), additionProposal);

    List<Note> liveNotes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(liveNotes, hasSize(1));
    Note copy = liveNotes.getFirst();
    NoteRealm copyView = noteController.showNote(copy);
    assertThat(copy.getTitle(), equalTo("Copy"));
    assertThat(copyView.getId(), not(equalTo(original.getId())));
    assertThat(copyView.getNote().getContent(), equalTo(COPIED_CONTENT));
    assertHasNoPrivateAssociations(copy);

    inCommittedTransaction(
        transactionManager,
        () -> {
          MemoryTracker tracker =
              memoryTrackerRepository.findById(associations.trackerId()).orElseThrow();
          assertThat(tracker.getNote().getId(), equalTo(original.getId()));
          Mcq mcq = mcqRepository.findById(associations.mcqId()).orElseThrow();
          assertThat(mcq.getNote().getId(), equalTo(original.getId()));
          Conversation conversation =
              conversationRepository.findById(associations.conversationId()).orElseThrow();
          assertThat(conversation.getSubject().getNote().getId(), equalTo(original.getId()));
        });
  }

  private void assertHasNoPrivateAssociations(Note note) throws Exception {
    assertThat(noteController.getNoteInfo(note).getMemoryTrackers(), empty());
    assertThat(mcqIdsForNote(note), empty());
    assertThat(conversationMessageController.getConversationsAboutNote(note), empty());
  }

  private OriginalAssociations commitPrivateAssociations(Note original) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          Note reloadedOriginal = noteRepository.findById(original.getId()).orElseThrow();
          MemoryTracker tracker = makeMe.aMemoryTrackerFor(reloadedOriginal).please();
          Mcq mcq = makeMe.anMcq().forNote(reloadedOriginal).please();
          Conversation conversation =
              makeMe
                  .aConversation()
                  .forANote(reloadedOriginal)
                  .from(currentUser.getUser())
                  .please();
          conversationId = conversation.getId();
          return new OriginalAssociations(tracker.getId(), mcq.getId(), conversation.getId());
        });
  }

  private List<Integer> mcqIdsForNote(Note note) {
    return StreamSupport.stream(mcqRepository.findAll().spliterator(), false)
        .filter(question -> question.getNote().getId().equals(note.getId()))
        .map(Mcq::getId)
        .toList();
  }

  private record OriginalAssociations(Integer trackerId, Integer mcqId, Integer conversationId) {}
}
