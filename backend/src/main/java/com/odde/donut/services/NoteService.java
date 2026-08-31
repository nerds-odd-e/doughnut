package com.odde.donut.services;

import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.donut.controllers.dto.NoteImageUploadDTO;
import com.odde.donut.controllers.dto.NoteImageUploadResult;
import com.odde.donut.entities.Image;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.ImageRepository;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.entities.repositories.ResolvedWikiLinkRepository;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.testability.TestabilitySettings;
import com.odde.donut.utils.ImageBuilder;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class NoteService {
  private final NoteRepository noteRepository;
  private final MemoryTrackerRepository memoryTrackerRepository;
  private final ImageRepository imageRepository;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;
  private final NoteReferenceHandling noteReferenceHandling;

  public NoteService(
      NoteRepository noteRepository,
      MemoryTrackerRepository memoryTrackerRepository,
      ResolvedWikiLinkRepository resolvedWikiLinkRepository,
      ResolvedWikiLinkService resolvedWikiLinkService,
      WikiLinkResolver wikiLinkResolver,
      AuthorizationService authorizationService,
      ImageRepository imageRepository,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings) {
    this.noteRepository = noteRepository;
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.imageRepository = imageRepository;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.noteReferenceHandling =
        new NoteReferenceHandling(
            memoryTrackerRepository,
            resolvedWikiLinkRepository,
            resolvedWikiLinkService,
            wikiLinkResolver,
            authorizationService,
            entityPersister,
            this::deleteOrphanImagesForPersistedContent);
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

  public void destroy(Note note, NoteDeleteReferenceHandling referenceHandling, User viewer) {
    destroy(note, referenceHandling, null, viewer);
  }

  public void destroy(
      Note note,
      NoteDeleteReferenceHandling referenceHandling,
      String sourcePropertyKey,
      User viewer) {
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    if (referenceHandling == NoteDeleteReferenceHandling.REDUCE_TO_SOURCE_PROPERTY) {
      noteReferenceHandling.reduceRelationNoteToSourceProperty(
          note, sourcePropertyKey, viewer, currentUTCTimestamp);
    } else if (referenceHandling == NoteDeleteReferenceHandling.REMOVE_FROM_PROPERTIES) {
      noteReferenceHandling.removeNoteLinksFromReferrerProperties(
          note, viewer, currentUTCTimestamp);
    }
    note.setUpdatedAt(currentUTCTimestamp);
    note.setDeletedAt(currentUTCTimestamp);
    entityPersister.merge(note);
    for (MemoryTracker mt : memoryTrackerRepository.findByNote_IdIn(List.of(note.getId()))) {
      mt.setDeletedAt(currentUTCTimestamp);
      entityPersister.merge(mt);
    }
  }

  /**
   * Deletes {@link Image} rows for this note that are not referenced by the saved {@code image:}
   * scalar in {@link Note#getContent()}, within the current transaction. Skips entirely when the
   * scalar is present but not a canonical attachment path.
   */
  public void deleteOrphanImagesForPersistedContent(Note note) {
    if (note == null || note.getId() == null) {
      return;
    }
    NoteContentMarkdown.LeadingFrontmatterImageReference ref =
        NoteContentMarkdown.leadingFrontmatterImageReference(note.getContent());
    if (ref instanceof NoteContentMarkdown.LeadingFrontmatterImageReference.InvalidPathPresent) {
      return;
    }
    Integer keepId =
        ref instanceof NoteContentMarkdown.LeadingFrontmatterImageReference.Referenced referenced
            ? referenced.imageId()
            : null;
    for (Image image : imageRepository.findByNote_Id(note.getId())) {
      if (keepId != null && keepId.equals(image.getId())) {
        continue;
      }
      entityPersister.remove(image);
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

  public NoteImageUploadResult uploadNoteImage(
      Note note, NoteImageUploadDTO noteImageUploadDTO, User user) throws IOException {
    Image image =
        new ImageBuilder().buildImageFromUploadedImage(user, noteImageUploadDTO.getUploadImage());
    image.setNote(note);
    entityPersister.save(image);
    entityPersister.flush();
    String imagePath = "/attachments/images/" + image.getId() + "/" + image.getName();
    return new NoteImageUploadResult(imagePath);
  }
}
