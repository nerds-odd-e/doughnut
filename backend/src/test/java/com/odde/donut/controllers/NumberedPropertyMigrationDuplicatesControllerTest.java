package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.PropertyFocus;
import com.odde.donut.entities.QuestionGenerationBatch;
import com.odde.donut.entities.QuestionGenerationBatchRequest;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.User;
import com.odde.donut.services.NumberedPropertyMigration;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NumberedPropertyMigrationDuplicatesControllerTest
    extends NotebookGitWebContentControllerTestBase {
  @Autowired NumberedPropertyMigration migration;
  @Autowired MemoryTrackerController trackerController;
  @Autowired ObjectMapper objectMapper;
  private Integer conversationId;

  @AfterEach
  void cleanupConversation() {
    if (conversationId != null) {
      inCommittedTransaction(
          transactionManager,
          () ->
              entityManager
                  .createNativeQuery("DELETE FROM conversation WHERE id = :id")
                  .setParameter("id", conversationId)
                  .executeUpdate());
    }
  }

  @Test
  void existingDestinationWinsAndDeletionRemovesTheCompleteForeignKeyClosure() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe
            .aNote()
            .notebook(notebook)
            .content("---\ntype: Note\ntopic: [Cafe]\ntopic 2: Café\n---\nBody")
            .please();
    DuplicateFixture fixture =
        inCommittedTransaction(
            transactionManager,
            () -> {
              Note stored = noteRepository.findById(note.getId()).orElseThrow();
              MemoryTracker redundant =
                  makeMe
                      .aMemoryTrackerFor(stored)
                      .propertyKey("topic 2")
                      .afterNthStrictRecall(3)
                      .recallCount(3)
                      .please();
              MemoryTracker retained =
                  makeMe
                      .aMemoryTrackerFor(stored)
                      .propertyKey("TOPIC")
                      .propertyValue("CAFE")
                      .removedFromTracking()
                      .afterNthStrictRecall(2)
                      .recallCount(2)
                      .please();
              RecallPrompt prompt =
                  makeMe
                      .aRecallPrompt()
                      .forMemoryTracker(redundant)
                      .withMcqForNote(stored)
                      .please();
              Conversation conversation =
                  makeMe
                      .aConversation()
                      .forARecallPrompt(prompt)
                      .from(redundant.getUser())
                      .please();
              conversationId = conversation.getId();
              QuestionGenerationBatch batch =
                  makeMe.aQuestionGenerationBatch().forUser(redundant.getUser()).please();
              QuestionGenerationBatchRequest request =
                  makeMe
                      .aQuestionGenerationBatchRequest()
                      .batch(batch)
                      .memoryTracker(redundant)
                      .please();
              return new DuplicateFixture(
                  redundant.getId(),
                  retained.getId(),
                  prompt.getId(),
                  conversation.getId(),
                  request.getId());
            });
    SurvivorState before = state(fixture.retained());
    snapshotCurrentPortableTree(notebook);

    assertThat(
        migration.migrateNotebook(notebook.getId(), testabilitySettings.getCurrentUTCTimestamp()),
        nullValue());

    assertThat(state(fixture.retained()), equalTo(before));
    inCommittedTransaction(
        transactionManager,
        () -> {
          assertThat(memoryTrackerRepository.existsById(fixture.redundant()), equalTo(false));
          assertThat(countRecallLogsByTrackerId(fixture.redundant()), equalTo(0L));
          assertThat(entityManager.find(RecallPrompt.class, fixture.prompt()), nullValue());
          assertThat(
              entityManager.find(QuestionGenerationBatchRequest.class, fixture.request()),
              nullValue());
          Conversation conversation =
              entityManager.find(Conversation.class, fixture.conversation());
          assertThat(conversation.getSubject(), nullValue());
          assertThat(
              memoryTrackerRepository.findByNote_IdIn(List.of(note.getId())).stream()
                  .map(MemoryTracker::getId)
                  .toList(),
              equalTo(List.of(fixture.retained())));
        });
  }

  @Test
  void lowestIdWinsWhenAllDestinationsMoveWithoutCombiningLearnersOrTrackerTypes()
      throws Exception {
    User other = inCommittedTransaction(transactionManager, this::createFixtureUser);
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe
            .aNote()
            .notebook(notebook)
            .content("---\ntype: Note\ntopic 2: Cafe\ntopic 9: Café\n---\nBody")
            .please();
    List<MemoryTracker> trackers =
        inCommittedTransaction(
            transactionManager,
            () -> {
              Note stored = noteRepository.findById(note.getId()).orElseThrow();
              return List.of(
                  makeMe
                      .aMemoryTrackerFor(stored)
                      .propertyKey("topic 9")
                      .afterNthStrictRecall(2)
                      .recallCount(2)
                      .please(),
                  makeMe
                      .aMemoryTrackerFor(stored)
                      .propertyKey("topic 2")
                      .removedFromTracking()
                      .please(),
                  makeMe
                      .aMemoryTrackerFor(stored)
                      .propertyKey("topic 2")
                      .by(entityManager.find(User.class, other.getId()))
                      .please(),
                  makeMe.aMemoryTrackerFor(stored).propertyKey("topic 2").spelling().please(),
                  makeMe.aMemoryTrackerFor(stored).propertyKey("topic 2").commissioned().please());
            });
    SurvivorState before = state(trackers.getFirst().getId());
    snapshotCurrentPortableTree(notebook);

    assertThat(
        migration.migrateNotebook(notebook.getId(), testabilitySettings.getCurrentUTCTimestamp()),
        nullValue());

    SurvivorState after = state(trackers.getFirst().getId());
    assertThat(
        after,
        equalTo(
            new SurvivorState(
                new PropertyFocus("topic", "Café"),
                before.next(),
                before.last(),
                before.assimilated(),
                before.stability(),
                before.difficulty(),
                before.removed(),
                before.history())));
    inCommittedTransaction(
        transactionManager,
        () -> {
          assertThat(memoryTrackerRepository.existsById(trackers.get(1).getId()), equalTo(false));
          assertThat(
              memoryTrackerRepository.findByNote_IdIn(List.of(note.getId())).stream()
                  .map(MemoryTracker::getId)
                  .sorted()
                  .toList(),
              equalTo(
                  List.of(
                          trackers.get(0).getId(),
                          trackers.get(2).getId(),
                          trackers.get(3).getId(),
                          trackers.get(4).getId())
                      .stream()
                      .sorted()
                      .toList()));
          for (MemoryTracker original : trackers.subList(2, 5)) {
            MemoryTracker retained =
                memoryTrackerRepository.findById(original.getId()).orElseThrow();
            assertThat(retained.propertyFocus(), equalTo(new PropertyFocus("topic", "Cafe")));
            assertThat(retained.getUser().getId(), equalTo(original.getUser().getId()));
            assertThat(retained.getType(), equalTo(original.getType()));
          }
        });
  }

  private SurvivorState state(Integer id) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          MemoryTracker tracker = memoryTrackerRepository.findById(id).orElseThrow();
          try {
            MemoryTracker shown = trackerController.showMemoryTracker(tracker);
            return new SurvivorState(
                shown.propertyFocus(),
                shown.getNextRecallAt(),
                shown.getLastRecalledAt(),
                shown.getAssimilatedAt(),
                shown.getStability(),
                shown.getDifficulty(),
                shown.getRemovedFromTracking(),
                objectMapper.writeValueAsString(trackerController.getRecallHistory(shown)));
          } catch (Exception exception) {
            throw new IllegalStateException(exception);
          }
        });
  }

  private record DuplicateFixture(
      Integer redundant, Integer retained, Integer prompt, Integer conversation, Integer request) {}

  private record SurvivorState(
      PropertyFocus focus,
      Timestamp next,
      Timestamp last,
      Timestamp assimilated,
      Float stability,
      Float difficulty,
      Boolean removed,
      String history) {}
}
