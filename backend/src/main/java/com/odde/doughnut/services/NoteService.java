package com.odde.doughnut.services;

import com.odde.doughnut.entities.LinkType;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.List;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

@Service
public class NoteService {
  private final NoteRepository noteRepository;
  private final EntityManager entityManager;
  private final EntityPersister entityPersister;

  public NoteService(
      NoteRepository noteRepository, EntityManager entityManager, EntityPersister entityPersister) {
    this.noteRepository = noteRepository;
    this.entityManager = entityManager;
    this.entityPersister = entityPersister;
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

  public Note createLink(
      Note sourceNote,
      Note targetNote,
      User creator,
      LinkType type,
      Timestamp currentUTCTimestamp) {
    if (type == null || type == LinkType.NO_LINK) return null;
    Note link = buildALink(sourceNote, targetNote, creator, type, currentUTCTimestamp);
    entityPersister.save(link);
    return link;
  }

  public static Note buildALink(
      Note sourceNote,
      Note targetNote,
      User creator,
      LinkType type,
      Timestamp currentUTCTimestamp) {
    final Note note = new Note();
    note.initialize(creator, sourceNote, currentUTCTimestamp, ":" + type.label);
    note.setTargetNote(targetNote);
    note.getRecallSetting()
        .setLevel(
            Math.max(
                sourceNote.getRecallSetting().getLevel(),
                targetNote.getRecallSetting().getLevel()));

    return note;
  }
}
