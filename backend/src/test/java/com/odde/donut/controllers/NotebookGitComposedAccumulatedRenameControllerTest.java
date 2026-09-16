package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Conversation;
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
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verifies that publishing a multi-commit accumulated rename range retains the accepted note
 * identity and learning data when each adjacent rename step clears the JGit similarity threshold
 * but the single-shot endpoint of the range falls below it, so identity must be carried through the
 * range rather than detected at the tip.
 */
class NotebookGitComposedAccumulatedRenameControllerTest
    extends NotebookGitBundleControllerTestBase {

  /**
   * Accumulated-rename fixture bodies (10 substantial prose lines each). {@link
   * #BODY_A_AT_ORIGINAL} and {@link #BODY_B_AFTER_FIRST_EDIT} share the last six lines (A→B
   * similarity >= 50); {@link #BODY_B_AFTER_FIRST_EDIT} and {@link #BODY_C_AFTER_SECOND_EDIT} share
   * the first four plus the last two lines (B→C similarity >= 50); {@link #BODY_A_AT_ORIGINAL} and
   * {@link #BODY_C_AFTER_SECOND_EDIT} share only the last two lines (A→C similarity below 50), so a
   * single-shot tip detector would not pair them and identity must be carried through the range.
   */
  private static final String BODY_A_AT_ORIGINAL =
      "---\ntype: Note\n---\n"
          + "The quick brown fox jumps over the lazy dog near the riverbank.\n"
          + "She decided to read the ancient manuscript that described the valley.\n"
          + "Mountains rose in the distance, their peaks covered with fresh snow.\n"
          + "A small village nestled between them kept its traditions alive for generations.\n"
          + "Travelers came each spring to trade cloth and spices at the market.\n"
          + "Children played near the fountain while elders discussed the harvest.\n"
          + "The librarian organized every scroll by region and by season.\n"
          + "Fishermen mended their nets along the quiet harbor at dawn.\n"
          + "Bakers prepared fresh bread before the first rooster crowed loudly.\n"
          + "Merchants recorded every transaction in thick leather-bound ledgers.\n";

  private static final String BODY_B_AFTER_FIRST_EDIT =
      "---\ntype: Note\n---\n"
          + "Wolves howled at the pale moon above the frozen northern tundra.\n"
          + "A wandering bard sang tales of old kingdoms beyond the sea.\n"
          + "Blacksmiths forged shining blades in the heat of the busy forge.\n"
          + "Farmers planted golden wheat across the rolling southern hills.\n"
          + "Travelers came each spring to trade cloth and spices at the market.\n"
          + "Children played near the fountain while elders discussed the harvest.\n"
          + "The librarian organized every scroll by region and by season.\n"
          + "Fishermen mended their nets along the quiet harbor at dawn.\n"
          + "Bakers prepared fresh bread before the first rooster crowed loudly.\n"
          + "Merchants recorded every transaction in thick leather-bound ledgers.\n";

  private static final String BODY_C_AFTER_SECOND_EDIT =
      "---\ntype: Note\n---\n"
          + "Wolves howled at the pale moon above the frozen northern tundra.\n"
          + "A wandering bard sang tales of old kingdoms beyond the sea.\n"
          + "Blacksmiths forged shining blades in the heat of the busy forge.\n"
          + "Farmers planted golden wheat across the rolling southern hills.\n"
          + "Astronomers charted distant galaxies through the great telescope.\n"
          + "Sailors navigated by the stars across the wide and stormy oceans.\n"
          + "Alchemists mixed rare herbs in pursuit of the legendary elixir.\n"
          + "Monks copied ancient texts by candlelight in the silent abbey.\n"
          + "Bakers prepared fresh bread before the first rooster crowed loudly.\n"
          + "Merchants recorded every transaction in thick leather-bound ledgers.\n";

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
  void publishesAccumulatedRenameEditsRetainingIdentityWhenEndpointBelowThreshold()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note original =
        makeMe.aNote().notebook(notebook).title("Original").content(BODY_A_AT_ORIGINAL).please();
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
      GitBundleTestReader.fetchHead(repository, binding.getBundleBytes());
      ObjectId afterFirstRename =
          commitOnTopOf(
              repository,
              List.of(acceptedHead),
              List.of(new NotebookGitProposalFile("Renamed.md", BODY_B_AFTER_FIRST_EDIT)),
              "Rename and edit note once");
      tip =
          commitOnTopOf(
              repository,
              List.of(afterFirstRename),
              List.of(new NotebookGitProposalFile("Final.md", BODY_C_AFTER_SECOND_EDIT)),
              "Rename and edit note again");
      proposalBytes = bundleBytesForHead(repository, tip);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    assertThat(publishedHead, equalTo(tip.getName()));
    Note retained = noteRepository.findById(original.getId()).orElseThrow();
    assertThat(retained.getTitle(), equalTo("Final"));
    assertThat(retained.getContent(), equalTo(BODY_C_AFTER_SECOND_EDIT));
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

  /**
   * Confirms the {@link #BODY_A_AT_ORIGINAL}→{@link #BODY_C_AFTER_SECOND_EDIT} endpoint of the
   * accumulated-rename fixture is genuinely below the 50 threshold: a single-shot rename with those
   * bodies is refused, so the range test above succeeds only through carried adjacent identity.
   */
  @Test
  void refusesSingleShotEndpointOfAccumulatedRenameFixtureAsBelowThreshold() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("Original").content(BODY_A_AT_ORIGINAL).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("Final.md", BODY_C_AFTER_SECOND_EDIT)));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposalBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("identity correspondence is uncertain"));
  }

  private record LearnedAssociations(Integer mcqId, Integer conversationId) {}
}
