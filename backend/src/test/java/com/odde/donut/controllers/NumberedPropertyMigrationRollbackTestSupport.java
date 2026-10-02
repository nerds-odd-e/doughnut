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
import org.springframework.beans.factory.annotation.Autowired;

/** Committed observations of the migration's private, derived and accepted state. */
abstract class NumberedPropertyMigrationRollbackTestSupport
    extends NotebookGitWebContentControllerTestBase {
  @Autowired MemoryTrackerController trackerController;
  @Autowired ObjectMapper objectMapper;
  @Autowired NotePropertyIndexRepository propertyIndexRepository;
  Integer conversationId;

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
