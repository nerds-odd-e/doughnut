package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.NoteAccessoriesDTO;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteAccessory;
import com.odde.doughnut.entities.NoteType;
import com.odde.doughnut.entities.RelationType;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

@Service
public class NoteService {
  private final NoteRepository noteRepository;
  private final MemoryTrackerRepository memoryTrackerRepository;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;

  public NoteService(
      NoteRepository noteRepository,
      MemoryTrackerRepository memoryTrackerRepository,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings) {
    this.noteRepository = noteRepository;
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
  }

  public List<Note> findRecentNotesByUser(Integer userId) {
    return noteRepository.findRecentNotesByUser(userId);
  }

  public Optional<Note> findById(Integer id) {
    return noteRepository.findById(id);
  }

  public void destroy(Note note) {
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    note.setUpdatedAt(currentUTCTimestamp);
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

    softDeleteMemoryTrackersForNotes(
        collectNoteIdsForDeletion(note, descendants, inboundReferences));
  }

  private List<Integer> collectNoteIdsForDeletion(
      Note note, List<Note> descendants, List<Note> inboundReferences) {
    List<Integer> noteIds = new ArrayList<>();
    noteIds.add(note.getId());
    descendants.forEach(d -> noteIds.add(d.getId()));
    inboundReferences.forEach(r -> noteIds.add(r.getId()));
    for (Note descendant : descendants) {
      noteRepository.findAllByTargetNote(descendant.getId()).forEach(r -> noteIds.add(r.getId()));
    }
    return noteIds;
  }

  private void softDeleteMemoryTrackersForNotes(List<Integer> noteIds) {
    if (noteIds.isEmpty()) return;
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    for (MemoryTracker mt : memoryTrackerRepository.findByNote_IdIn(noteIds)) {
      mt.setDeletedAt(currentUTCTimestamp);
      entityPersister.merge(mt);
    }
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
      List<Integer> noteIdsToRestore = collectNoteIdsToRestore(note, deletedAt);
      restoreMemoryTrackersForNotes(noteIdsToRestore, deletedAt);

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

  private List<Integer> collectNoteIdsToRestore(Note note, Timestamp deletedAt) {
    Set<Integer> noteIds = new LinkedHashSet<>();
    collectNoteIdsToRestoreInto(noteIds, note, deletedAt);
    return new ArrayList<>(noteIds);
  }

  private void collectNoteIdsToRestoreInto(Set<Integer> noteIds, Note note, Timestamp deletedAt) {
    if (deletedAt.equals(note.getDeletedAt())) noteIds.add(note.getId());
    noteRepository.findAllByTargetNote(note.getId()).stream()
        .filter(r -> deletedAt.equals(r.getDeletedAt()))
        .forEach(r -> noteIds.add(r.getId()));
    for (Note child : noteRepository.findAllByParentId(note.getId())) {
      if (deletedAt.equals(child.getDeletedAt())) {
        noteIds.add(child.getId());
        noteRepository.findAllByTargetNote(child.getId()).stream()
            .filter(r -> deletedAt.equals(r.getDeletedAt()))
            .forEach(r -> noteIds.add(r.getId()));
        collectNoteIdsToRestoreInto(noteIds, child, deletedAt);
      }
    }
  }

  private void restoreMemoryTrackersForNotes(List<Integer> noteIds, Timestamp deletedAt) {
    if (noteIds.isEmpty()) return;
    for (MemoryTracker mt : memoryTrackerRepository.findByNote_IdIn(noteIds)) {
      if (sameTimestamp(deletedAt, mt.getDeletedAt())) {
        mt.setDeletedAt(null);
        entityPersister.merge(mt);
      }
    }
  }

  private boolean sameTimestamp(Timestamp a, Timestamp b) {
    if (a == null || b == null) return a == b;
    return Math.abs(a.getTime() - b.getTime()) < 1000;
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

  public Note createRelationship(
      Note sourceNote,
      Note targetNote,
      User creator,
      RelationType type,
      Timestamp currentUTCTimestamp) {
    if (type == null) return null;
    Note relation = buildARelation(sourceNote, targetNote, creator, type, currentUTCTimestamp);
    entityPersister.save(relation);
    return relation;
  }

  public static Note buildARelation(
      Note sourceNote,
      Note targetNote,
      User creator,
      RelationType type,
      Timestamp currentUTCTimestamp) {
    final Note note = new Note();
    note.initialize(creator, sourceNote, currentUTCTimestamp, null);
    note.setTargetNote(targetNote);
    note.setRelationType(type);
    note.getRecallSetting()
        .setLevel(
            Math.max(
                sourceNote.getRecallSetting().getLevel(),
                targetNote.getRecallSetting().getLevel()));

    return note;
  }

  public void setNoteType(Note note, NoteType noteType) {
    note.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());
    note.setNoteType(noteType);
    entityPersister.merge(note);
  }

  public NoteAccessory updateNoteAccessories(
      Note note, NoteAccessoriesDTO noteAccessoriesDTO, User user) throws IOException {
    note.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());
    note.getOrInitializeNoteAccessory().setFromDTO(noteAccessoriesDTO, user);
    entityPersister.save(note);
    return note.getNoteAccessory();
  }
}
