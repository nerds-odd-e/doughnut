package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.List;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

@Service
public class NoteService {
  private final NoteRepository noteRepository;
  private final EntityManager entityManager;

  public NoteService(NoteRepository noteRepository, EntityManager entityManager) {
    this.noteRepository = noteRepository;
    this.entityManager = entityManager;
  }

  public void destroy(Note note, Timestamp currentUTCTimestamp) {
    if (note.getNotebook() != null) {
      if (note.getNotebook().getHeadNote() == note) {
        note.getNotebook().setDeletedAt(currentUTCTimestamp);
        entityManager.merge(note.getNotebook());
      }
    }

    note.setDeletedAt(currentUTCTimestamp);
    entityManager.merge(note);
  }

  public void restore(Note note) {
    if (note.getNotebook() != null) {
      if (note.getNotebook().getHeadNote() == note) {
        note.getNotebook().setDeletedAt(null);
        entityManager.merge(note.getNotebook());
      }
    }
    note.setDeletedAt(null);
    entityManager.merge(note);
  }

  public boolean hasDuplicateWikidataId(Note note) {
    if (Strings.isEmpty(note.getWikidataId())) {
      return false;
    }
    List<Note> existingNotes =
        noteRepository.noteWithWikidataIdWithinNotebook(
            note.getNotebook().getId(), note.getWikidataId());
    return (existingNotes.stream().anyMatch(n -> !n.equals(note)));
  }
}
