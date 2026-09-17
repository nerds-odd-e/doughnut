package com.odde.donut.controllers;

import com.odde.donut.entities.Note;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;

/** Row counts across a note's dependent closure, for deletion/retry absence and survival proofs. */
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

  /** Aggregates the complete note-dependent closure counts for absence/survival assertions. */
  protected DependentCounts dependentCounts(Note note) {
    return new DependentCounts(
        countRowsByNoteId("memory_tracker", note.getId()),
        countRecallPromptsByNoteId(note.getId()),
        countRowsByNoteId("mcq", note.getId()),
        countRowsByNoteId("image", note.getId()),
        countRowsByNoteId("conversation", note.getId()),
        countConversationMessagesByNoteId(note.getId()));
  }

  /** Snapshot of the complete note-dependent row counts used by deletion/retry proofs. */
  protected record DependentCounts(
      long memoryTracker,
      long recallPrompt,
      long mcq,
      long image,
      long conversation,
      long conversationMessage) {
    static DependentCounts allAbsent() {
      return new DependentCounts(0, 0, 0, 0, 0, 0);
    }
  }
}
