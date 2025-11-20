package com.odde.doughnut.services;

import com.odde.doughnut.entities.LinkType;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

@Service
public class NoteService {
  private final NoteRepository noteRepository;
  private final EntityPersister entityPersister;
  private final MemoryTrackerService memoryTrackerService;

  public NoteService(
      NoteRepository noteRepository,
      EntityPersister entityPersister,
      MemoryTrackerService memoryTrackerService) {
    this.noteRepository = noteRepository;
    this.entityPersister = entityPersister;
    this.memoryTrackerService = memoryTrackerService;
  }

  public List<Note> findRecentNotesByUser(Integer userId) {
    return noteRepository.findRecentNotesByUser(userId);
  }

  public Optional<Note> findById(Integer id) {
    return noteRepository.findById(id);
  }

  public void destroy(Note note, Timestamp currentUTCTimestamp) {
    List<Note> noteAndDescendants =
        Stream.concat(Stream.of(note), note.getAllDescendants()).toList();

    if (note.getNotebook() != null) {
      if (note.getNotebook().getHeadNote() == note) {
        note.getNotebook().setDeletedAt(currentUTCTimestamp);
        entityPersister.merge(note.getNotebook());
      }
    }

    note.setDeletedAt(currentUTCTimestamp);
    entityPersister.merge(note);

    memoryTrackerService.softDeleteMemoryTrackersForNotes(noteAndDescendants);
  }

  public void restore(Note note) {
    List<Note> noteAndDescendants =
        Stream.concat(Stream.of(note), note.getAllDescendants()).toList();

    if (note.getNotebook() != null) {
      if (note.getNotebook().getHeadNote() == note) {
        note.getNotebook().setDeletedAt(null);
        entityPersister.merge(note.getNotebook());
      }
    }
    note.setDeletedAt(null);
    entityPersister.merge(note);

    memoryTrackerService.restoreMemoryTrackersForNotes(noteAndDescendants);
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
