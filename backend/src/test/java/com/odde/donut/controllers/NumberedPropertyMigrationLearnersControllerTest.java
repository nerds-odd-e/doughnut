package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.PropertyFocus;
import com.odde.donut.entities.User;
import com.odde.donut.services.NumberedPropertyMigration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NumberedPropertyMigrationLearnersControllerTest
    extends NotebookGitWebContentControllerTestBase {
  @Autowired NumberedPropertyMigration migration;
  @Autowired MemoryTrackerController memoryTrackerController;

  @Test
  void everyLearnersPersistedTrackerFollowsIncludingInactiveUnrelatedAndNoteLevelFocuses()
      throws Exception {
    User owner = currentUser.getUser();
    User otherLearner = inCommittedTransaction(transactionManager, this::createFixtureUser);
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe
            .aNote()
            .notebook(notebook)
            .content("---\ntype: Note\ntopic: A\ntopic 2: B\nother: C\n---\nBody")
            .please();
    List<MemoryTracker> trackers =
        inCommittedTransaction(
            transactionManager,
            () -> {
              Note stored = noteRepository.findById(note.getId()).orElseThrow();
              User storedLearner = entityManager.find(User.class, otherLearner.getId());
              return List.of(
                  makeMe
                      .aMemoryTrackerFor(stored)
                      .propertyKey("topic 2")
                      .afterNthStrictRecall(2)
                      .please(),
                  makeMe
                      .aMemoryTrackerFor(stored)
                      .by(storedLearner)
                      .propertyKey("topic 2")
                      .removedFromTracking()
                      .please(),
                  makeMe.aMemoryTrackerFor(stored).by(storedLearner).propertyKey("other").please(),
                  makeMe.aMemoryTrackerFor(stored).by(storedLearner).please());
            });
    snapshotCurrentPortableTree(notebook);

    assertThat(
        migration.migrateNotebook(notebook.getId(), testabilitySettings.getCurrentUTCTimestamp()),
        nullValue());

    try {
      assertShownTracker(owner, trackers.get(0), new PropertyFocus("topic", "B"), false);
      assertShownTracker(otherLearner, trackers.get(1), new PropertyFocus("topic", "B"), true);
      assertShownTracker(otherLearner, trackers.get(2), new PropertyFocus("other", ""), false);
      assertShownTracker(otherLearner, trackers.get(3), null, false);
    } finally {
      currentUser.setUser(owner);
    }
  }

  private void assertShownTracker(
      User learner, MemoryTracker original, PropertyFocus expectedFocus, boolean inactive) {
    currentUser.setUser(learner);
    inCommittedTransaction(
        transactionManager,
        () -> {
          try {
            MemoryTracker shown =
                memoryTrackerController.showMemoryTracker(
                    memoryTrackerRepository.findById(original.getId()).orElseThrow());
            assertThat(shown.getId(), equalTo(original.getId()));
            assertThat(shown.getUser().getId(), equalTo(learner.getId()));
            assertThat(shown.propertyFocus(), equalTo(expectedFocus));
            assertThat(shown.getRemovedFromTracking(), equalTo(inactive));
          } catch (Exception exception) {
            throw new IllegalStateException(exception);
          }
        });
  }
}
