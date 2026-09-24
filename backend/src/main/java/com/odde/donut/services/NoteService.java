package com.odde.donut.services;

import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.controllers.dto.NoteTrashReferenceHandling;
import com.odde.donut.entities.Image;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.testability.TestabilitySettings;
import jakarta.persistence.FlushModeType;
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
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;
  private final NoteReferenceHandling noteReferenceHandling;
  private final NoteReferenceService noteReferenceService;

  public NoteService(
      NoteRepository noteRepository,
      MemoryTrackerRepository memoryTrackerRepository,
      NoteReferenceService noteReferenceService,
      WikiLinkResolver wikiLinkResolver,
      AuthorizationService authorizationService,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings) {
    this.noteRepository = noteRepository;
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.noteReferenceService = noteReferenceService;
    this.noteReferenceHandling =
        new NoteReferenceHandling(
            memoryTrackerRepository,
            noteReferenceService,
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

  /**
   * Permanently removes {@code note} and its complete dependent data. The note row is hard-deleted;
   * note-owned dependents (memory_tracker, recall_prompt, mcq, image, conversation, and
   * authored_note_reference source rows) are removed by their ON DELETE CASCADE foreign keys. The
   * reference-handling contract is applied first so authored inbound text in referrer notes is
   * preserved for {@link NoteTrashReferenceHandling#LEAVE_DEAD_LINKS}.
   */
  public void permanentlyRemove(
      Note note, NoteTrashReferenceHandling referenceHandling, User viewer) {
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    applyNoteReferenceHandling(note, referenceHandling, viewer, currentUTCTimestamp);
    entityPersister.remove(note);
  }

  /**
   * The note {@code relationNote}'s relationship source resolves to for {@code viewer}; refuses
   * with 400 when it is unresolvable or not editable.
   */
  public Note resolveRelationshipSource(Note relationNote, User viewer) {
    return noteReferenceHandling.resolveRelationshipSource(relationNote, viewer);
  }

  /**
   * Adds {@code relationNote}'s relationship as a property on its resolved source note, deriving
   * the property key from the note's own {@code relation} frontmatter, and rehomes every learner's
   * note-level understanding tracker onto that property. Returns the source note; does not remove
   * {@code relationNote} itself.
   */
  public Note reduceRelationNoteToSourceProperty(
      Note relationNote, User viewer, Timestamp updatedAt) {
    return noteReferenceHandling.reduceRelationNoteToSourceProperty(
        relationNote, viewer, updatedAt);
  }

  public void applyNoteReferenceHandling(
      Note note, NoteTrashReferenceHandling referenceHandling, User viewer) {
    applyNoteReferenceHandling(
        note, referenceHandling, viewer, testabilitySettings.getCurrentUTCTimestamp());
  }

  private void applyNoteReferenceHandling(
      Note note, NoteTrashReferenceHandling referenceHandling, User viewer, Timestamp updatedAt) {
    if (referenceHandling == NoteTrashReferenceHandling.REMOVE_FROM_PROPERTIES) {
      noteReferenceHandling.removeNoteLinksFromReferrerProperties(note, viewer, updatedAt);
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
    entityPersister
        .createQuery(
            "FROM Image i WHERE i.note = :note AND (:keepId IS NULL OR i.id <> :keepId)",
            Image.class)
        .setParameter("note", note)
        .setParameter("keepId", keepId)
        .setFlushMode(FlushModeType.COMMIT)
        .getResultList()
        .forEach(entityPersister::remove);
  }
}
