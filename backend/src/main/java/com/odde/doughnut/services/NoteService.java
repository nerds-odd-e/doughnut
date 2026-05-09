package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.NoteContentMarkdown;
import com.odde.doughnut.controllers.dto.NoteAccessoriesDTO;
import com.odde.doughnut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteAccessory;
import com.odde.doughnut.entities.NoteWikiTitleCache;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.NoteWikiTitleCacheRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class NoteService {
  private final NoteRepository noteRepository;
  private final MemoryTrackerRepository memoryTrackerRepository;
  private final NoteWikiTitleCacheRepository noteWikiTitleCacheRepository;
  private final WikiTitleCacheService wikiTitleCacheService;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;

  public NoteService(
      NoteRepository noteRepository,
      MemoryTrackerRepository memoryTrackerRepository,
      NoteWikiTitleCacheRepository noteWikiTitleCacheRepository,
      WikiTitleCacheService wikiTitleCacheService,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings) {
    this.noteRepository = noteRepository;
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.noteWikiTitleCacheRepository = noteWikiTitleCacheRepository;
    this.wikiTitleCacheService = wikiTitleCacheService;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
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

  /**
   * Structural peers (same folder, or notebook root when {@code anchor} has no folder), excluding
   * the anchor, optional focus note, and {@code excludeNoteIds}, capped at {@code cap} rows from
   * the database. Without a sample seed, peers are ordered by id ascending; with a seed, order is
   * deterministic for that seed (CRC32-based) so repeated calls match.
   */
  public List<Note> findStructuralPeerNotesSample(
      Note anchor,
      Integer focusNoteId,
      Set<Integer> excludeNoteIds,
      int cap,
      Optional<Long> sampleSeed) {
    if (cap <= 0) {
      return List.of();
    }
    List<Integer> excludeIds = structuralPeerExcludeIds(anchor, focusNoteId, excludeNoteIds);
    if (anchor.getFolder() != null && anchor.getFolder().getId() != null) {
      Integer folderId = anchor.getFolder().getId();
      return sampleSeed
          .map(
              seed ->
                  noteRepository.findStructuralPeersInFolderOrderBySeedLimited(
                      folderId, excludeIds, Long.toString(seed), cap))
          .orElseGet(
              () ->
                  noteRepository.findStructuralPeersInFolderOrderByIdAscLimited(
                      folderId, excludeIds, cap));
    }
    if (anchor.getNotebook() == null || anchor.getNotebook().getId() == null) {
      return List.of();
    }
    Integer notebookId = anchor.getNotebook().getId();
    return sampleSeed
        .map(
            seed ->
                noteRepository.findStructuralPeersInNotebookRootOrderBySeedLimited(
                    notebookId, excludeIds, Long.toString(seed), cap))
        .orElseGet(
            () ->
                noteRepository.findStructuralPeersInNotebookRootOrderByIdAscLimited(
                    notebookId, excludeIds, cap));
  }

  private static List<Integer> structuralPeerExcludeIds(
      Note anchor, Integer focusNoteId, Set<Integer> excludeNoteIds) {
    LinkedHashSet<Integer> ids = new LinkedHashSet<>();
    if (anchor.getId() != null) {
      ids.add(anchor.getId());
    }
    if (focusNoteId != null) {
      ids.add(focusNoteId);
    }
    for (Integer id : excludeNoteIds) {
      if (id != null) {
        ids.add(id);
      }
    }
    if (ids.isEmpty()) {
      return List.of(-1);
    }
    return List.copyOf(ids);
  }

  public void destroy(Note note) {
    destroy(note, NoteDeleteReferenceHandling.LEAVE_DEAD_LINKS, note.getCreator());
  }

  public void destroy(Note note, NoteDeleteReferenceHandling referenceHandling) {
    destroy(note, referenceHandling, note.getCreator());
  }

  public void destroy(Note note, NoteDeleteReferenceHandling referenceHandling, User viewer) {
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    if (referenceHandling == NoteDeleteReferenceHandling.REMOVE_FROM_PROPERTIES) {
      removeNoteLinksFromReferrerProperties(note, viewer, currentUTCTimestamp);
    }
    note.setUpdatedAt(currentUTCTimestamp);
    note.setDeletedAt(currentUTCTimestamp);
    entityPersister.merge(note);
    for (MemoryTracker mt : memoryTrackerRepository.findByNote_IdIn(List.of(note.getId()))) {
      mt.setDeletedAt(currentUTCTimestamp);
      entityPersister.merge(mt);
    }
  }

  private void removeNoteLinksFromReferrerProperties(
      Note target, User viewer, Timestamp updatedAt) {
    Map<Note, Set<String>> referrersByLinkTexts = new LinkedHashMap<>();
    for (NoteWikiTitleCache row :
        noteWikiTitleCacheRepository.findRowsReferringToNonDeletedNotesForTarget(target.getId())) {
      referrersByLinkTexts
          .computeIfAbsent(row.getNote(), ignored -> new LinkedHashSet<>())
          .add(row.getLinkText());
    }
    for (Map.Entry<Note, Set<String>> entry : referrersByLinkTexts.entrySet()) {
      Note referrer = entry.getKey();
      NoteContentMarkdown.removeWikiLinksFromLeadingFrontmatterProperties(
              referrer.getContent(), entry.getValue())
          .ifPresent(
              updatedContent -> {
                referrer.setContent(updatedContent);
                referrer.setUpdatedAt(updatedAt);
                entityPersister.merge(referrer);
                wikiTitleCacheService.refreshForNote(referrer, viewer);
              });
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

  public NoteAccessory updateNoteAccessories(
      Note note, NoteAccessoriesDTO noteAccessoriesDTO, User user) throws IOException {
    note.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());
    note.getOrInitializeNoteAccessory().setFromDTO(noteAccessoriesDTO, user);
    entityPersister.save(note);
    return note.getNoteAccessory();
  }
}
