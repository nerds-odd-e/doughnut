package com.odde.doughnut.services;

import com.odde.doughnut.entities.LinkType;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

@Service
public class NoteService {
  private final NoteRepository noteRepository;
  private final EntityPersister entityPersister;

  public NoteService(NoteRepository noteRepository, EntityPersister entityPersister) {
    this.noteRepository = noteRepository;
    this.entityPersister = entityPersister;
  }

  public List<Note> findRecentNotesByUser(Integer userId) {
    return noteRepository.findRecentNotesByUser(userId);
  }

  public Optional<Note> findById(Integer id) {
    return noteRepository.findById(id);
  }

  public void destroy(Note note, Timestamp currentUTCTimestamp) {
    if (note.getNotebook() != null) {
      if (note.getNotebook().getHeadNote() == note) {
        note.getNotebook().setDeletedAt(currentUTCTimestamp);
        entityPersister.merge(note.getNotebook());
      }
    }

    // Delete all descendants recursively
    List<Note> descendants = note.getAllDescendants().toList();
    for (Note descendant : descendants) {
      descendant.setDeletedAt(currentUTCTimestamp);
      entityPersister.merge(descendant);
    }

    // Delete all inbound references to the note itself
    List<Note> inboundReferences = noteRepository.findAllByTargetNote(note.getId());
    for (Note reference : inboundReferences) {
      reference.setDeletedAt(currentUTCTimestamp);
      entityPersister.merge(reference);
    }

    // Delete all inbound references to all descendants
    for (Note descendant : descendants) {
      List<Note> descendantReferences = noteRepository.findAllByTargetNote(descendant.getId());
      for (Note reference : descendantReferences) {
        reference.setDeletedAt(currentUTCTimestamp);
        entityPersister.merge(reference);
      }
    }

    note.setDeletedAt(currentUTCTimestamp);
    entityPersister.merge(note);
  }

  public void restore(Note note) {
    if (note.getNotebook() != null) {
      if (note.getNotebook().getHeadNote() == note) {
        note.getNotebook().setDeletedAt(null);
        entityPersister.merge(note.getNotebook());
      }
    }

    Timestamp deletedAt = note.getDeletedAt();
    if (deletedAt != null) {
      // Restore all descendants that were deleted at the same time (cascaded deletion)
      restoreDescendantsRecursively(note, deletedAt);

      // Restore all inbound references that were deleted at the same time
      List<Note> inboundReferences = noteRepository.findAllByTargetNote(note.getId());
      for (Note reference : inboundReferences) {
        if (deletedAt.equals(reference.getDeletedAt())) {
          reference.setDeletedAt(null);
          entityPersister.merge(reference);
        }
      }

      // Restore all inbound references to descendants that were deleted at the same time
      restoreDescendantReferencesRecursively(note, deletedAt);
    }

    note.setDeletedAt(null);
    entityPersister.merge(note);
  }

  private void restoreDescendantsRecursively(Note note, Timestamp deletedAt) {
    List<Note> children = noteRepository.findAllByParentId(note.getId());
    for (Note child : children) {
      if (deletedAt.equals(child.getDeletedAt())) {
        child.setDeletedAt(null);
        entityPersister.merge(child);
        restoreDescendantsRecursively(child, deletedAt);
      }
    }
  }

  private void restoreDescendantReferencesRecursively(Note note, Timestamp deletedAt) {
    List<Note> children = noteRepository.findAllByParentId(note.getId());
    for (Note child : children) {
      if (deletedAt.equals(child.getDeletedAt())) {
        List<Note> descendantReferences = noteRepository.findAllByTargetNote(child.getId());
        for (Note reference : descendantReferences) {
          if (deletedAt.equals(reference.getDeletedAt())) {
            reference.setDeletedAt(null);
            entityPersister.merge(reference);
          }
        }
        restoreDescendantReferencesRecursively(child, deletedAt);
      }
    }
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
