package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.entities.AuthoredNoteReferenceRow;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.PropertyFocus;
import com.odde.donut.entities.repositories.AuthoredNoteReferenceRowTestSupport;
import com.odde.donut.entities.repositories.NotePropertyIndexRepository;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

/** Committed fixtures and observations for migration atomicity and retry behavior. */
abstract class NumberedPropertyMigrationCommittedTestSupport
    extends NotebookGitWebContentControllerTestBase {
  @Autowired MemoryTrackerController trackerController;
  @Autowired ObjectMapper objectMapper;
  @Autowired NotePropertyIndexRepository propertyIndexRepository;
  Integer conversationId;

  @AfterEach
  void resetFailureAndCleanupConversation() {
    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(false);
    NotebookGitPublicationAtomicTestSupport.FAIL_FOR_NOTEBOOK_ID.set(null);
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

  List<Integer> seedDuplicateWithClosure(Note note) {
    return inCommittedTransaction(
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
          MemoryTracker survivor =
              makeMe
                  .aMemoryTrackerFor(stored)
                  .propertyKey("topic")
                  .propertyValue("A")
                  .afterNthStrictRecall(2)
                  .recallCount(2)
                  .please();
          var prompt =
              makeMe.aRecallPrompt().forMemoryTracker(redundant).withMcqForNote(stored).please();
          conversationId =
              makeMe
                  .aConversation()
                  .forARecallPrompt(prompt)
                  .from(redundant.getUser())
                  .please()
                  .getId();
          var batch = makeMe.aQuestionGenerationBatch().forUser(redundant.getUser()).please();
          makeMe.aQuestionGenerationBatchRequest().batch(batch).memoryTracker(redundant).please();
          return List.of(redundant.getId(), survivor.getId());
        });
  }

  NoteState noteState(Note note) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          try {
            Note stored = noteRepository.findById(note.getId()).orElseThrow();
            var shown = noteController.showNote(stored);
            return new NoteState(
                shown.getNote().getContent(),
                stored.getUpdatedAt(),
                objectMapper.writeValueAsString(shown.getWikiLinks()),
                AuthoredNoteReferenceRowTestSupport.rowsFor(entityManager, stored).stream()
                    .map(AuthoredNoteReferenceRow::getAuthoredLink)
                    .toList(),
                propertyIndexRepository.findByNote_IdOrderByIdAsc(stored.getId()).stream()
                    .map(
                        index ->
                            new PropertyFocus(index.getPropertyKey(), index.getPropertyValue()))
                    .toList(),
                memoryTrackerRepository.findByNote_IdIn(List.of(stored.getId())).stream()
                    .map(MemoryTracker::getId)
                    .sorted()
                    .toList());
          } catch (Exception exception) {
            throw new IllegalStateException(exception);
          }
        });
  }

  Learning learning(Integer id) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          try {
            MemoryTracker shown =
                trackerController.showMemoryTracker(
                    memoryTrackerRepository.findById(id).orElseThrow());
            return new Learning(
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

  String deletionClosure(Integer trackerId) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          try {
            var logs =
                entityManager
                    .createNativeQuery(
                        "SELECT * FROM recall_log WHERE memory_tracker_id = :id ORDER BY id")
                    .setParameter("id", trackerId)
                    .getResultList();
            var prompts =
                entityManager
                    .createNativeQuery(
                        "SELECT * FROM recall_prompt WHERE memory_tracker_id = :id ORDER BY id")
                    .setParameter("id", trackerId)
                    .getResultList();
            var requests =
                entityManager
                    .createNativeQuery(
                        "SELECT * FROM question_generation_batch_request WHERE memory_tracker_id = :id ORDER BY id")
                    .setParameter("id", trackerId)
                    .getResultList();
            var conversationPrompt =
                entityManager
                    .createNativeQuery("SELECT recall_prompt_id FROM conversation WHERE id = :id")
                    .setParameter("id", conversationId)
                    .getSingleResult();
            assertThat(logs.size(), equalTo(3));
            assertThat(prompts.size(), equalTo(1));
            assertThat(requests.size(), equalTo(1));
            return objectMapper.writeValueAsString(
                List.of(logs, prompts, requests, conversationPrompt));
          } catch (Exception exception) {
            throw new IllegalStateException(exception);
          }
        });
  }

  BindingState bindingState(Notebook notebook) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          var binding =
              notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
          return new BindingState(
              binding.getAcceptedGitObjectId(),
              binding.getUpdatedAt(),
              countNativeObjectStoreRows(binding.getId()));
        });
  }

  record NoteState(
      String content,
      Timestamp updated,
      String links,
      List<String> references,
      List<PropertyFocus> properties,
      List<Integer> trackerIds) {}

  record Learning(
      PropertyFocus focus,
      Timestamp next,
      Timestamp last,
      Timestamp assimilated,
      Float stability,
      Float difficulty,
      Boolean removed,
      String history) {}

  record BindingState(String head, Timestamp updated, long nativeRows) {}
}
