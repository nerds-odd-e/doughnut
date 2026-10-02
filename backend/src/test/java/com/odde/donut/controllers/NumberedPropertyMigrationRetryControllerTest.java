package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.PropertyFocus;
import com.odde.donut.services.NumberedPropertyMigration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test", "notebook-git-publication-atomic-test"})
@Import(NotebookGitPublicationAtomicTestSupport.FailingBindingSaveConfig.class)
class NumberedPropertyMigrationRetryControllerTest
    extends NumberedPropertyMigrationCommittedTestSupport {
  @Autowired NumberedPropertyMigration migration;

  @Test
  void interruptedRunResumesRemainingNotebookAndRepeatedRunKeepsAcceptedLearningAndDeletionStable()
      throws Exception {
    Notebook firstBook = createGitBackedNotebook("First");
    Note first = legacyNote(firstBook, "[A]", "A");
    Notebook secondBook = createGitBackedNotebook("Second");
    Note second = legacyNote(secondBook, "A", "B");
    List<Integer> firstTrackers = seedDuplicateWithClosure(first);
    Integer secondTracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(second.getId()).orElseThrow())
                    .propertyKey("topic 2")
                    .afterNthStrictRecall(2)
                    .recallCount(2)
                    .please()
                    .getId());
    snapshotCurrentPortableTree(firstBook);
    snapshotCurrentPortableTree(secondBook);
    var firstOriginalHistory = acceptedHistory(firstBook);
    var secondOriginalHistory = acceptedHistory(secondBook);
    var secondOriginal = noteState(second);
    var secondOriginalBinding = bindingState(secondBook);
    var secondOriginalLearning = learning(secondTracker);
    var survivorOriginal = learning(firstTrackers.getLast());
    deletionClosure(firstTrackers.getFirst());

    NotebookGitPublicationAtomicTestSupport.FAIL_FOR_NOTEBOOK_ID.set(secondBook.getId());
    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(true);
    var failure =
        assertThrows(
            RuntimeException.class,
            () -> migration.run(testabilitySettings.getCurrentUTCTimestamp()));
    assertThat(failure.getMessage(), equalTo("forced failure after note projection"));
    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(false);
    NotebookGitPublicationAtomicTestSupport.FAIL_FOR_NOTEBOOK_ID.set(null);

    var firstCompleted = noteState(first);
    var firstBinding = bindingState(firstBook);
    var firstHistory = acceptedHistory(firstBook);
    assertThat(firstCompleted.content(), equalTo("---\ntype: Note\ntopic: [\"A\"]\n---\nCarrier"));
    assertThat(firstCompleted.trackerIds(), equalTo(List.of(firstTrackers.getLast())));
    assertThat(firstHistory.parents(), equalTo(firstOriginalHistory.commits()));
    assertThat(learning(firstTrackers.getLast()), equalTo(survivorOriginal));
    assertDuplicateClosureDeleted(firstTrackers.getFirst());
    assertThat(noteState(second), equalTo(secondOriginal));
    assertThat(bindingState(secondBook), equalTo(secondOriginalBinding));
    assertThat(acceptedHistory(secondBook), equalTo(secondOriginalHistory));
    assertThat(learning(secondTracker), equalTo(secondOriginalLearning));

    assertThat(migration.run(testabilitySettings.getCurrentUTCTimestamp()), equalTo(Map.of()));
    assertThat(noteState(first), equalTo(firstCompleted));
    assertThat(bindingState(firstBook), equalTo(firstBinding));
    assertThat(acceptedHistory(firstBook), equalTo(firstHistory));
    var secondCompleted = noteState(second);
    var secondBinding = bindingState(secondBook);
    var secondHistory = acceptedHistory(secondBook);
    var secondLearning = learning(secondTracker);
    assertThat(
        secondCompleted.content(), equalTo("---\ntype: Note\ntopic: [\"A\", \"B\"]\n---\nCarrier"));
    assertThat(secondCompleted.trackerIds(), equalTo(List.of(secondTracker)));
    assertThat(secondHistory.parents(), equalTo(secondOriginalHistory.commits()));
    assertThat(secondLearning.focus(), equalTo(new PropertyFocus("topic", "B")));
    assertThat(secondLearning.history(), equalTo(secondOriginalLearning.history()));
    assertThat(secondLearning.next(), equalTo(secondOriginalLearning.next()));

    assertThat(migration.run(testabilitySettings.getCurrentUTCTimestamp()), equalTo(Map.of()));
    assertThat(noteState(first), equalTo(firstCompleted));
    assertThat(bindingState(firstBook), equalTo(firstBinding));
    assertThat(acceptedHistory(firstBook), equalTo(firstHistory));
    assertThat(learning(firstTrackers.getLast()), equalTo(survivorOriginal));
    assertDuplicateClosureDeleted(firstTrackers.getFirst());
    assertThat(noteState(second), equalTo(secondCompleted));
    assertThat(bindingState(secondBook), equalTo(secondBinding));
    assertThat(acceptedHistory(secondBook), equalTo(secondHistory));
    assertThat(learning(secondTracker), equalTo(secondLearning));
  }

  Note legacyNote(Notebook book, String base, String extra) {
    return makeMe
        .aNote()
        .notebook(book)
        .title("Carrier")
        .content("---\ntype: Note\ntopic: " + base + "\ntopic 2: " + extra + "\n---\nCarrier")
        .please();
  }

  void assertDuplicateClosureDeleted(Integer id) {
    inCommittedTransaction(
        transactionManager,
        () -> {
          assertThat(memoryTrackerRepository.findById(id).isEmpty(), is(true));
          for (String table :
              List.of("recall_log", "recall_prompt", "question_generation_batch_request")) {
            var count =
                (Number)
                    entityManager
                        .createNativeQuery(
                            "SELECT COUNT(*) FROM " + table + " WHERE memory_tracker_id = :id")
                        .setParameter("id", id)
                        .getSingleResult();
            assertThat(count.longValue(), equalTo(0L));
          }
          var count =
              (Number)
                  entityManager
                      .createNativeQuery(
                          "SELECT COUNT(*) FROM conversation WHERE id = :id AND recall_prompt_id IS NULL")
                      .setParameter("id", conversationId)
                      .getSingleResult();
          assertThat(count.longValue(), equalTo(1L));
        });
  }
}
