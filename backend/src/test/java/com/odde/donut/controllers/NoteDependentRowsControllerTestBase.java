package com.odde.donut.controllers;

import com.odde.donut.entities.Note;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/** Selected learning and conversation row counts for note deletion/retry proofs. */
abstract class NoteDependentRowsControllerTestBase extends NotebookControllerTestBase {
  @Autowired EntityManager entityManager;

  /** Counts rows in {@code table} that reference the given note via {@code note_id}. */
  protected long countRowsByNoteId(String table, Integer noteId) {
    return ((Number)
            entityManager
                .createNativeQuery("SELECT COUNT(*) FROM " + table + " WHERE note_id = :id")
                .setParameter("id", noteId)
                .getSingleResult())
        .longValue();
  }

  /** Counts recall prompts whose memory tracker belongs to the given note. */
  protected long countRecallPromptsByNoteId(Integer noteId) {
    return ((Number)
            entityManager
                .createNativeQuery(
                    "SELECT COUNT(*) FROM recall_prompt rp "
                        + "JOIN memory_tracker mt ON rp.memory_tracker_id = mt.id "
                        + "WHERE mt.note_id = :id")
                .setParameter("id", noteId)
                .getSingleResult())
        .longValue();
  }

  /** Counts conversation_message rows belonging to any conversation of the given note. */
  protected long countConversationMessagesByNoteId(Integer noteId) {
    return ((Number)
            entityManager
                .createNativeQuery(
                    "SELECT COUNT(*) FROM conversation_message cm "
                        + "JOIN conversation c ON cm.conversation_id = c.id "
                        + "WHERE c.note_id = :id")
                .setParameter("id", noteId)
                .getSingleResult())
        .longValue();
  }

  protected List<Integer> questionIdsForNote(Note note) {
    return entityManager
        .createQuery(
            "SELECT DISTINCT prompt.mcq.id FROM RecallPrompt prompt "
                + "WHERE prompt.memoryTracker.note = :note AND prompt.mcq IS NOT NULL",
            Integer.class)
        .setParameter("note", note)
        .getResultList();
  }

  protected long countQuestionsByIds(List<Integer> questionIds) {
    return entityManager
        .createQuery(
            "SELECT COUNT(question) FROM Mcq question WHERE question.id IN :ids", Long.class)
        .setParameter("ids", questionIds)
        .getSingleResult();
  }

  /** Aggregates selected learning and conversation counts for deletion/survival assertions. */
  protected DependentCounts dependentCounts(Note note, List<Integer> questionIds) {
    return new DependentCounts(
        countRowsByNoteId("memory_tracker", note.getId()),
        countRecallPromptsByNoteId(note.getId()),
        countQuestionsByIds(questionIds),
        countRowsByNoteId("conversation", note.getId()),
        countConversationMessagesByNoteId(note.getId()),
        countRowsByNoteId("conversation_note_correction", note.getId()));
  }

  /** Snapshot of selected learning and conversation counts used by deletion/retry proofs. */
  protected record DependentCounts(
      long memoryTracker,
      long recallPrompt,
      long mcq,
      long conversation,
      long conversationMessage,
      long conversationNoteCorrection) {
    static DependentCounts allAbsent() {
      return new DependentCounts(0, 0, 0, 0, 0, 0);
    }
  }
}
