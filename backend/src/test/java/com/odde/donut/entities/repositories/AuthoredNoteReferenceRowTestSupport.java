package com.odde.donut.entities.repositories;

import com.odde.donut.entities.AuthoredNoteReferenceRow;
import com.odde.donut.entities.Note;
import jakarta.persistence.EntityManager;
import java.util.List;

/** Reads source-owned {@code authored_note_reference} rows for a note. */
public final class AuthoredNoteReferenceRowTestSupport {
  private AuthoredNoteReferenceRowTestSupport() {}

  public static List<AuthoredNoteReferenceRow> rowsFor(EntityManager entityManager, Note note) {
    entityManager.flush();
    return entityManager
        .createQuery(
            "FROM AuthoredNoteReferenceRow r WHERE r.note.id = :id ORDER BY r.documentOrder",
            AuthoredNoteReferenceRow.class)
        .setParameter("id", note.getId())
        .getResultList();
  }
}
