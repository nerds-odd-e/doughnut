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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
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
  private final NoteChildContainerFolderService noteChildContainerFolderService;
  private final WikiTitleCacheService wikiTitleCacheService;
  private final RelationshipNoteEndpointResolver relationshipNoteEndpointResolver;

  public NoteService(
      NoteRepository noteRepository,
      MemoryTrackerRepository memoryTrackerRepository,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      NoteChildContainerFolderService noteChildContainerFolderService,
      WikiTitleCacheService wikiTitleCacheService,
      RelationshipNoteEndpointResolver relationshipNoteEndpointResolver) {
    this.noteRepository = noteRepository;
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.noteChildContainerFolderService = noteChildContainerFolderService;
    this.wikiTitleCacheService = wikiTitleCacheService;
    this.relationshipNoteEndpointResolver = relationshipNoteEndpointResolver;
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
    return noteRepository.findNotesInFolderOrderBySiblingOrder(folderId);
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

    List<Note> cluster = collectSoftDeleteCluster(note);
    for (Note member : cluster) {
      member.setUpdatedAt(currentUTCTimestamp);
      member.setDeletedAt(currentUTCTimestamp);
      entityPersister.merge(member);
    }

    List<Note> inboundReferences = new ArrayList<>();
    for (Note member : cluster) {
      inboundReferences.addAll(noteRepository.findAllByTargetNote(member.getId()));
    }
    for (Note reference : inboundReferences) {
      reference.setDeletedAt(currentUTCTimestamp);
      entityPersister.merge(reference);
    }

    softDeleteMemoryTrackersForNotes(collectNoteIdsForDeletion(cluster));
  }

  /**
   * Notes soft-deleted with {@code root}: {@code root} plus relationship carriers reachable from
   * {@code root} through successive {@link Note#isRelation()} children only (not structural
   * note-tree containment).
   */
  private List<Note> collectSoftDeleteCluster(Note root) {
    List<Note> out = new ArrayList<>();
    Set<Integer> seen = new HashSet<>();
    Deque<Note> q = new ArrayDeque<>();
    q.add(root);
    seen.add(root.getId());
    while (!q.isEmpty()) {
      Note n = q.removeFirst();
      out.add(n);
      for (Note child : noteRepository.findAllByParentId(n.getId())) {
        if (!child.isRelation() || !seen.add(child.getId())) {
          continue;
        }
        q.add(child);
      }
    }
    return out;
  }

  private List<Integer> collectNoteIdsForDeletion(List<Note> cluster) {
    Set<Integer> noteIds = new LinkedHashSet<>();
    for (Note member : cluster) {
      noteIds.add(member.getId());
      noteRepository.findAllByTargetNote(member.getId()).forEach(r -> noteIds.add(r.getId()));
    }
    return new ArrayList<>(noteIds);
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
    if (!deletedAt.equals(note.getDeletedAt())) {
      return;
    }
    Set<Integer> visited = new HashSet<>();
    Deque<Note> q = new ArrayDeque<>();
    q.add(note);
    visited.add(note.getId());
    while (!q.isEmpty()) {
      Note n = q.removeFirst();
      noteIds.add(n.getId());
      noteRepository.findAllByTargetNote(n.getId()).stream()
          .filter(r -> deletedAt.equals(r.getDeletedAt()))
          .forEach(r -> noteIds.add(r.getId()));
      for (Note child : noteRepository.findAllByParentId(n.getId())) {
        if (!deletedAt.equals(child.getDeletedAt()) || !child.isRelation()) {
          continue;
        }
        if (visited.add(child.getId())) {
          q.add(child);
        }
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
    Deque<Note> q = new ArrayDeque<>();
    for (Note child : noteRepository.findAllByParentId(note.getId())) {
      if (deletedAt.equals(child.getDeletedAt()) && child.isRelation()) {
        q.add(child);
      }
    }
    while (!q.isEmpty()) {
      Note n = q.removeFirst();
      n.setDeletedAt(null);
      entityPersister.merge(n);
      for (Note child : noteRepository.findAllByParentId(n.getId())) {
        if (deletedAt.equals(child.getDeletedAt()) && child.isRelation()) {
          q.add(child);
        }
      }
    }
  }

  private void restoreDescendantReferencesRecursively(Note note, Timestamp deletedAt) {
    Deque<Note> q = new ArrayDeque<>();
    for (Note child : noteRepository.findAllByParentId(note.getId())) {
      if (deletedAt.equals(child.getDeletedAt()) && child.isRelation()) {
        q.add(child);
      }
    }
    while (!q.isEmpty()) {
      Note n = q.removeFirst();
      for (Note reference : noteRepository.findAllByTargetNote(n.getId())) {
        if (deletedAt.equals(reference.getDeletedAt())) {
          reference.setDeletedAt(null);
          entityPersister.merge(reference);
        }
      }
      for (Note child : noteRepository.findAllByParentId(n.getId())) {
        if (deletedAt.equals(child.getDeletedAt()) && child.isRelation()) {
          q.add(child);
        }
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

  /**
   * Rebuilds title and relationship markdown from {@code relationType} (from the API on update, or
   * the type chosen at creation).
   */
  public void refreshRelationshipNoteTitle(Note relation, RelationType relationType, User viewer) {
    if (relationType == null) {
      return;
    }
    Note source = relation.getParent();
    Note target =
        relationshipNoteEndpointResolver
            .resolveSemanticTarget(relation, viewer)
            .orElseGet(relation::getTargetNote);
    relation.setTitle(
        RelationshipNoteTitleFormatter.format(
            source.getTitle(), relationType.label, target.getTitle()));
    String preserved =
        RelationshipNoteMarkdownFormatter.extractUserSuffixFromRelationshipDetails(
            relation.getDetails());
    relation.setDetails(
        RelationshipNoteMarkdownFormatter.formatForRelationshipNote(
            relation, relationType, source, target, preserved));
    relation.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());
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
