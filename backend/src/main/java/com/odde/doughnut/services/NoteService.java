package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.NoteAccessoriesDTO;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteAccessory;
import com.odde.doughnut.entities.RelationType;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

@Service
public class NoteService {
  private final NoteRepository noteRepository;
  private final MemoryTrackerRepository memoryTrackerRepository;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;
  private final NoteChildContainerFolderService noteChildContainerFolderService;
  private final WikiTitleCacheService wikiTitleCacheService;

  public NoteService(
      NoteRepository noteRepository,
      MemoryTrackerRepository memoryTrackerRepository,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      NoteChildContainerFolderService noteChildContainerFolderService,
      WikiTitleCacheService wikiTitleCacheService) {
    this.noteRepository = noteRepository;
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.noteChildContainerFolderService = noteChildContainerFolderService;
    this.wikiTitleCacheService = wikiTitleCacheService;
  }

  public List<Note> findRecentNotesByUser(Integer userId) {
    return noteRepository.findRecentNotesByUser(userId);
  }

  public Optional<Note> findById(Integer id) {
    return noteRepository.findById(id);
  }

  public List<Note> findNotebookRootNotes(Integer notebookId) {
    return noteRepository.findNotesInNotebookRootFolderScopeByNotebookId(notebookId);
  }

  public List<Note> findNotesInFolderScope(Integer folderId) {
    return noteRepository.findNotesInFolderOrderByIdAsc(folderId);
  }

  /**
   * Notes in the same structural scope as {@code note}: its folder, or notebook root when no
   * folder.
   */
  public List<Note> findStructuralPeerNotesInOrder(Note note) {
    if (note.getFolder() != null) {
      return findNotesInFolderScope(note.getFolder().getId());
    }
    return findNotebookRootNotes(note.getNotebook().getId());
  }

  public void destroy(Note note) {
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    note.setUpdatedAt(currentUTCTimestamp);
    note.setDeletedAt(currentUTCTimestamp);
    entityPersister.merge(note);
    for (MemoryTracker mt : memoryTrackerRepository.findByNote_IdIn(List.of(note.getId()))) {
      mt.setDeletedAt(currentUTCTimestamp);
      entityPersister.merge(mt);
    }
  }

  public void restore(Note note) {
    Timestamp deletedAt = note.getDeletedAt();
    if (deletedAt != null) {
      for (MemoryTracker mt : memoryTrackerRepository.findByNote_IdIn(List.of(note.getId()))) {
        if (sameTimestamp(deletedAt, mt.getDeletedAt())) {
          mt.setDeletedAt(null);
          entityPersister.merge(mt);
        }
      }
    }
    note.setDeletedAt(null);
    entityPersister.merge(note);
  }

  private boolean sameTimestamp(Timestamp a, Timestamp b) {
    if (a == null || b == null) return a == b;
    return Math.abs(a.getTime() - b.getTime()) < 1000;
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
    relation.setTitle(
        RelationshipNoteTitleFormatter.format(
            sourceNote.getTitle(), type.label, targetNote.getTitle()));
    relation.setDetails(
        RelationshipNoteMarkdownFormatter.formatForRelationshipNote(
            relation, type, sourceNote, targetNote, null));
    relation.setFolder(noteChildContainerFolderService.resolveForParent(sourceNote));
    entityPersister.save(relation);
    wikiTitleCacheService.refreshForNote(relation, creator);
    return relation;
  }

  public static Note buildARelation(
      Note sourceNote,
      Note targetNote,
      User creator,
      RelationType type,
      Timestamp currentUTCTimestamp) {
    final Note note = new Note();
    note.initializeNewNote(creator, sourceNote.getNotebook(), currentUTCTimestamp, null);
    note.getRecallSetting()
        .setLevel(
            Math.max(
                sourceNote.getRecallSetting().getLevel(),
                targetNote.getRecallSetting().getLevel()));

    return note;
  }

  public NoteAccessory updateNoteAccessories(
      Note note, NoteAccessoriesDTO noteAccessoriesDTO, User user) throws IOException {
    note.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());
    note.getOrInitializeNoteAccessory().setFromDTO(noteAccessoriesDTO, user);
    entityPersister.save(note);
    return note.getNoteAccessory();
  }
}
