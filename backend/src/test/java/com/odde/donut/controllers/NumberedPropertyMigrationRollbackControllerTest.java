package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.services.NumberedPropertyMigration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test", "notebook-git-publication-atomic-test"})
@Import(NotebookGitPublicationAtomicTestSupport.FailingBindingSaveConfig.class)
class NumberedPropertyMigrationRollbackControllerTest
    extends NumberedPropertyMigrationCommittedTestSupport {
  private static final String OLD_ITEM = "Read [[Migration Target:Carrier#prop:topic%202|detail]]";
  @Autowired NumberedPropertyMigration migration;

  @Test
  void latePublicationFailureRestoresBothTreesPrivateLearningAndDeletedForeignKeyClosure()
      throws Exception {
    Notebook targetBook = createGitBackedNotebook("Migration Target");
    Note target =
        makeMe
            .aNote()
            .notebook(targetBook)
            .title("Carrier")
            .content("---\ntype: Note\ntopic: [A]\ntopic 2: A\n---\nCarrier")
            .please();
    Notebook sourceBook = createGitBackedNotebook("References");
    Note source =
        makeMe
            .aNote()
            .notebook(sourceBook)
            .title("Source")
            .content("---\ntype: Note\nabout: ['" + OLD_ITEM + "']\n---\nSource")
            .please();
    List<Integer> duplicateTrackers = seedDuplicateWithClosure(target);
    Integer sourceTracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(source.getId()).orElseThrow())
                    .propertyKey("about")
                    .propertyValue(OLD_ITEM)
                    .afterNthStrictRecall(2)
                    .recallCount(2)
                    .please()
                    .getId());
    List<Integer> trackers =
        List.of(duplicateTrackers.getFirst(), duplicateTrackers.getLast(), sourceTracker);
    snapshotCurrentPortableTree(targetBook);
    snapshotCurrentPortableTree(sourceBook);
    var targetHistory = acceptedHistory(targetBook);
    var sourceHistory = acceptedHistory(sourceBook);
    var targetBefore = noteState(target);
    var sourceBefore = noteState(source);
    var learningBefore = trackers.stream().map(this::learning).toList();
    var closureBefore = deletionClosure(trackers.getFirst());
    var targetBinding = bindingState(targetBook);
    var sourceBinding = bindingState(sourceBook);
    assertThat(noteController.showNote(source).getWikiLinks(), hasSize(1));

    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(true);
    RuntimeException failure =
        assertThrows(
            RuntimeException.class,
            () ->
                migration.migrateNotebook(
                    targetBook.getId(), testabilitySettings.getCurrentUTCTimestamp()));
    assertThat(failure.getMessage(), equalTo("forced failure after note projection"));
    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(false);

    assertThat(noteState(target), equalTo(targetBefore));
    assertThat(noteState(source), equalTo(sourceBefore));
    assertThat(trackers.stream().map(this::learning).toList(), equalTo(learningBefore));
    assertThat(deletionClosure(trackers.getFirst()), equalTo(closureBefore));
    assertThat(bindingState(targetBook), equalTo(targetBinding));
    assertThat(bindingState(sourceBook), equalTo(sourceBinding));
    assertThat(acceptedHistory(targetBook), equalTo(targetHistory));
    assertThat(acceptedHistory(sourceBook), equalTo(sourceHistory));
  }
}
