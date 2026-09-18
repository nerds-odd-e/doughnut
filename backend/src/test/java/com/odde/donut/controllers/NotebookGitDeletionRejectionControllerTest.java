package com.odde.donut.controllers;

import static com.odde.donut.controllers.NotebookGitRenameScoringBodies.SUBSTANTIAL_ORIGINAL_BODY;
import static com.odde.donut.controllers.NotebookGitRenameScoringBodies.SUBSTANTIAL_UNRELATED_BODY;
import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookGitDeletionRejectionControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String ORIGINAL = "---\ntype: Note\n---\noriginal content";
  private static final String OTHER = "---\ntype: Note\n---\nother content";
  private static final String EDITED = "---\ntype: Note\n---\nedited content";
  private static final String INVALID_EDIT = "---\ncustom: value\n---\nChanged body.\n";

  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @ParameterizedTest
  @MethodSource("identityUncertainDeletionProposals")
  void refusesMixingRemovalsWithAdditionsWithoutChangingAcceptedNotes(
      List<NotebookGitProposalFile> proposedFiles, List<String> expectedAffectedPaths)
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    List<Integer> originalIds =
        inCommittedTransaction(
            transactionManager,
            () ->
                List.of(
                    makeMe
                        .aNote()
                        .notebook(notebook)
                        .title("First")
                        .content(ORIGINAL)
                        .please()
                        .getId(),
                    makeMe
                        .aNote()
                        .notebook(notebook)
                        .title("Second")
                        .content(ORIGINAL)
                        .please()
                        .getId(),
                    makeMe
                        .aNote()
                        .notebook(notebook)
                        .title("Third")
                        .content(OTHER)
                        .please()
                        .getId()));
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            binding.getAcceptedGitObjectId(),
            proposalBundleBytes(binding, proposedFiles),
            HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("identity correspondence is uncertain"));
    assertThat(exception.getReason(), containsString("affected paths:"));
    for (String path : expectedAffectedPaths) {
      assertThat(exception.getReason(), containsString(path));
    }
    inCommittedTransaction(
        transactionManager,
        () -> {
          List<Note> storedNotes = noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId());
          assertThat(storedNotes.stream().map(Note::getId).toList(), equalTo(originalIds));
          assertThat(
              storedNotes.stream().map(Note::getContent).toList(),
              equalTo(List.of(ORIGINAL, ORIGINAL, OTHER)));
        });
  }

  static Stream<Arguments> identityUncertainDeletionProposals() {
    NotebookGitProposalFile second = new NotebookGitProposalFile("Second.md", ORIGINAL);
    NotebookGitProposalFile third = new NotebookGitProposalFile("Third.md", OTHER);
    return Stream.of(
        // unequal-blob removal + addition
        Arguments.of(
            List.of(
                second, third, new NotebookGitProposalFile("Added.md", SUBSTANTIAL_UNRELATED_BODY)),
            List.of("First.md", "Added.md")),
        // one source and two equal-blob destinations
        Arguments.of(
            List.of(
                second,
                third,
                new NotebookGitProposalFile("MovedA.md", ORIGINAL),
                new NotebookGitProposalFile("MovedB.md", ORIGINAL)),
            List.of("First.md", "MovedA.md", "MovedB.md")),
        // two sources and one equal-blob destination
        Arguments.of(
            List.of(third, new NotebookGitProposalFile("Moved.md", ORIGINAL)),
            List.of("First.md", "Second.md", "Moved.md")),
        // two sources and two equal-blob destinations
        Arguments.of(
            List.of(
                third,
                new NotebookGitProposalFile("MovedA.md", ORIGINAL),
                new NotebookGitProposalFile("MovedB.md", ORIGINAL)),
            List.of("First.md", "Second.md", "MovedA.md", "MovedB.md")),
        // an ambiguity alongside a separate unique pair
        Arguments.of(
            List.of(
                new NotebookGitProposalFile("MovedA.md", ORIGINAL),
                new NotebookGitProposalFile("MovedB.md", ORIGINAL),
                new NotebookGitProposalFile("Unique.md", OTHER)),
            List.of("First.md", "Second.md", "MovedA.md", "MovedB.md")),
        // a unique pair plus residual unequal removal and addition
        Arguments.of(
            List.of(
                second,
                new NotebookGitProposalFile("Unique.md", OTHER),
                new NotebookGitProposalFile("Added.md", SUBSTANTIAL_UNRELATED_BODY)),
            List.of("First.md", "Added.md")));
  }

  @Test
  void refusalLeavesLearningAssociationsIntactOnCommittedRollback() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note original =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aNote()
                    .notebook(notebook)
                    .title("Original")
                    .content(SUBSTANTIAL_ORIGINAL_BODY)
                    .please());
    Integer trackerId =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(original.getId()).orElseThrow())
                    .nextRecallAt(makeMe.aTimestamp().of(5, 0).please())
                    .please()
                    .getId());
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            binding.getAcceptedGitObjectId(),
            proposalBundleBytes(
                binding,
                List.of(new NotebookGitProposalFile("Renamed.md", SUBSTANTIAL_UNRELATED_BODY))),
            HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("identity correspondence is uncertain"));
    assertThat(exception.getReason(), containsString("affected paths:"));
    inCommittedTransaction(
        transactionManager,
        () -> {
          Note surviving = noteRepository.findById(original.getId()).orElseThrow();
          assertThat(surviving.getContent(), equalTo(SUBSTANTIAL_ORIGINAL_BODY));
          assertThat(dependentCounts(surviving).memoryTracker(), equalTo(1L));
          assertThat(
              memoryTrackerRepository.findById(trackerId).orElseThrow().getNote().getId(),
              equalTo(original.getId()));
        });
  }

  @Test
  void refusesAMoveWhenACompanionEditIsInvalidWithoutChangingAcceptedNotes() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    List<Integer> originalIds =
        inCommittedTransaction(
            transactionManager,
            () ->
                List.of(
                    makeMe
                        .aNote()
                        .notebook(notebook)
                        .title("First")
                        .content(ORIGINAL)
                        .please()
                        .getId(),
                    makeMe
                        .aNote()
                        .notebook(notebook)
                        .title("Second")
                        .content(ORIGINAL)
                        .please()
                        .getId()));
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);

    assertProposalRejectedWithoutMutatingBinding(
        notebook,
        binding.getAcceptedGitObjectId(),
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Renamed.md", ORIGINAL),
                new NotebookGitProposalFile("Second.md", INVALID_EDIT))),
        HttpStatus.BAD_REQUEST);

    inCommittedTransaction(
        transactionManager,
        () -> {
          List<Note> storedNotes = noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId());
          assertThat(storedNotes.stream().map(Note::getId).toList(), equalTo(originalIds));
          assertThat(
              storedNotes.stream().map(Note::getContent).toList(),
              equalTo(List.of(ORIGINAL, ORIGINAL)));
          assertThat(
              storedNotes.stream().map(Note::getTitle).toList(),
              equalTo(List.of("First", "Second")));
        });
  }
}
