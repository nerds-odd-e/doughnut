package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Publishes a linear local range that moves a retained note into {@code _trash} and then to a final
 * active path. Identity and dependents stay on the original note; accepted history keeps the
 * intermediate trash commit. Returning to the original path is still that identity, not a
 * deletion-gap recreation.
 */
class NotebookGitComposedTrashMoveIdentityControllerTest
    extends NotebookGitWebContentControllerTestBase {

  private static final String CELLS_BODY = "---\ntype: Note\n---\ncells body";

  @Test
  void publishesTrashThenAlternateActivePathRetainingIdentityDependentsAndAncestry()
      throws Exception {
    SeededCells seed = seedBiologyCellsWithLearning();
    DependentCounts dependentsBefore =
        inCommittedTransaction(transactionManager, () -> dependentCounts(seed.cells()));
    ComposedTrashRange range =
        composeTrashThenFinal(
            acceptedBundleBytes(seed.notebook()),
            seed.acceptedHead(),
            List.of(
                new NotebookGitProposalFile("_trash/Biology/Cells.md", CELLS_BODY),
                new NotebookGitProposalFile("Biology/.keep", "")),
            List.of(
                new NotebookGitProposalFile("Research/Cells.md", CELLS_BODY),
                new NotebookGitProposalFile("Biology/.keep", "")));

    String publishedHead =
        controller.publishNotebookGitProposal(
            seed.notebook().getId(),
            seed.binding().getAcceptedGitObjectId(),
            range.proposalBytes());

    assertThat(publishedHead, equalTo(range.tip().getName()));
    Note relocated = noteRepository.findById(seed.cells().getId()).orElseThrow();
    assertThat(relocated.isTrashed(), is(false));
    assertThat(relocated.getTitle(), equalTo("Cells"));
    assertThat(relocated.getContent(), equalTo(CELLS_BODY));
    NoteRealm shown = noteController.showNote(relocated);
    assertThat(
        shown.getAncestorFolders().stream().map(Folder::getName).toList(), contains("Research"));
    MemoryTracker learned = memoryTrackerRepository.findById(seed.tracker().getId()).orElseThrow();
    assertThat(learned.getNote().getId(), equalTo(seed.cells().getId()));
    assertThat(learned.isActive(), is(true));
    assertThat(learned.getDifficulty(), equalTo(seed.tracker().getDifficulty()));
    assertThat(
        inCommittedTransaction(transactionManager, () -> dependentCounts(seed.cells())),
        equalTo(dependentsBefore));
    assertAcceptedIntermediateAncestry(seed.notebook(), seed.acceptedHead(), range);
  }

  @Test
  void publishesTrashThenReturnToOriginalPathRetainingIdentityNotRecreation() throws Exception {
    SeededCells seed = seedBiologyCellsWithLearning();
    ComposedTrashRange range =
        composeTrashThenFinal(
            acceptedBundleBytes(seed.notebook()),
            seed.acceptedHead(),
            List.of(
                new NotebookGitProposalFile("_trash/Biology/Cells.md", CELLS_BODY),
                new NotebookGitProposalFile("Biology/.keep", "")),
            List.of(new NotebookGitProposalFile("Biology/Cells.md", CELLS_BODY)));

    controller.publishNotebookGitProposal(
        seed.notebook().getId(), seed.binding().getAcceptedGitObjectId(), range.proposalBytes());

    Note returned = noteRepository.findById(seed.cells().getId()).orElseThrow();
    assertThat(returned.getTitle(), equalTo("Cells"));
    assertThat(returned.getFolder().getId(), equalTo(seed.biology().getId()));
    assertThat(
        noteRepository.findAllByNotebookIdOrderByIdAsc(seed.notebook().getId()).stream()
            .map(Note::getId)
            .toList(),
        contains(seed.cells().getId()));
    assertThat(
        memoryTrackerRepository.findById(seed.tracker().getId()).orElseThrow().getNote().getId(),
        equalTo(seed.cells().getId()));
    assertThat(
        inCommittedTransaction(transactionManager, () -> dependentCounts(seed.cells())),
        not(equalTo(DependentCounts.allAbsent())));
  }

  private SeededCells seedBiologyCellsWithLearning() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    Note cells = makeMe.aNote("Cells").folder(biology).content(CELLS_BODY).please();
    MemoryTracker tracker = learnedTracker(cells, 0.5f, 1);
    inCommittedTransaction(
        transactionManager,
        () -> {
          Note reloaded = noteRepository.findById(cells.getId()).orElseThrow();
          makeMe
              .aRecallPrompt()
              .forMemoryTracker(memoryTrackerRepository.findById(tracker.getId()).orElseThrow())
              .withMcqForNote(reloaded)
              .please();
          makeMe.anImage().forNote(reloaded).please();
          makeMe.aConversation().forANote(reloaded).please();
        });
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    return new SeededCells(
        notebook,
        biology,
        cells,
        tracker,
        binding,
        ObjectId.fromString(binding.getAcceptedGitObjectId()));
  }

  private static ComposedTrashRange composeTrashThenFinal(
      byte[] acceptedBundle,
      ObjectId acceptedHead,
      List<NotebookGitProposalFile> trashTree,
      List<NotebookGitProposalFile> finalTree)
      throws Exception {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.fetchHead(repository, acceptedBundle);
      ObjectId trashCommit =
          commitOnTopOf(repository, List.of(acceptedHead), trashTree, "Move note into trash");
      ObjectId tip =
          commitOnTopOf(repository, List.of(trashCommit), finalTree, "Move note to final path");
      return new ComposedTrashRange(trashCommit, tip, bundleBytesForHead(repository, tip));
    }
  }

  private void assertAcceptedIntermediateAncestry(
      Notebook notebook, ObjectId acceptedHead, ComposedTrashRange range) throws Exception {
    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository accepted = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk walk = new RevWalk(accepted)) {
      ObjectId head = GitBundleTestReader.fetchHead(accepted, downloaded.getBody());
      RevCommit tip = walk.parseCommit(head);
      assertThat(tip.getId(), equalTo(range.tip()));
      assertThat(
          GitBundleTestReader.pathsIn(accepted, range.tip()),
          containsInAnyOrder("Research/Cells.md", "Biology/.keep"));
      assertThat(tip.getParentCount(), equalTo(1));
      assertThat(tip.getParent(0), equalTo(range.trashCommit()));

      RevCommit trash = walk.parseCommit(tip.getParent(0));
      assertThat(
          GitBundleTestReader.pathsIn(accepted, range.trashCommit()),
          containsInAnyOrder("_trash/Biology/Cells.md", "Biology/.keep"));
      assertThat(trash.getParentCount(), equalTo(1));
      assertThat(trash.getParent(0), equalTo(acceptedHead));
    }
  }

  private record SeededCells(
      Notebook notebook,
      Folder biology,
      Note cells,
      MemoryTracker tracker,
      NotebookGitBinding binding,
      ObjectId acceptedHead) {}

  private record ComposedTrashRange(ObjectId trashCommit, ObjectId tip, byte[] proposalBytes) {}
}
