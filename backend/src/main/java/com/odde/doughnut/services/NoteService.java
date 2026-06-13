package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.Frontmatter;
import com.odde.doughnut.algorithms.NoteContentMarkdown;
import com.odde.doughnut.algorithms.PropertyKeyNaming;
import com.odde.doughnut.algorithms.WikiLinkMarkdown;
import com.odde.doughnut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.doughnut.controllers.dto.NoteImageUploadDTO;
import com.odde.doughnut.controllers.dto.NoteImageUploadResult;
import com.odde.doughnut.entities.Image;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteWikiTitleCache;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.ImageRepository;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.NoteWikiTitleCacheRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.utils.ImageBuilder;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NoteService {
  private final NoteRepository noteRepository;
  private final MemoryTrackerRepository memoryTrackerRepository;
  private final NoteWikiTitleCacheRepository noteWikiTitleCacheRepository;
  private final WikiTitleCacheService wikiTitleCacheService;
  private final WikiLinkResolver wikiLinkResolver;
  private final AuthorizationService authorizationService;
  private final ImageRepository imageRepository;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;

  private static final String RELATIONSHIP_NOTE_TYPE = "relationship";

  public NoteService(
      NoteRepository noteRepository,
      MemoryTrackerRepository memoryTrackerRepository,
      NoteWikiTitleCacheRepository noteWikiTitleCacheRepository,
      WikiTitleCacheService wikiTitleCacheService,
      WikiLinkResolver wikiLinkResolver,
      AuthorizationService authorizationService,
      ImageRepository imageRepository,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings) {
    this.noteRepository = noteRepository;
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.noteWikiTitleCacheRepository = noteWikiTitleCacheRepository;
    this.wikiTitleCacheService = wikiTitleCacheService;
    this.wikiLinkResolver = wikiLinkResolver;
    this.authorizationService = authorizationService;
    this.imageRepository = imageRepository;
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
      reduceRelationNoteToSourceProperty(note, sourcePropertyKey, viewer, currentUTCTimestamp);
    } else if (referenceHandling == NoteDeleteReferenceHandling.REMOVE_FROM_PROPERTIES) {
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

  private void reduceRelationNoteToSourceProperty(
      Note relationNote, String propertyKey, User viewer, Timestamp updatedAt) {
    if (propertyKey == null || propertyKey.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Property key is required to reduce a relationship note.");
    }
    RelationshipFrontmatter relationship =
        parseRelationshipFrontmatter(relationNote.getContent())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "This note is not a relationship note."));
    Note sourceNote =
        resolveRelationshipSourceNote(relationNote, relationship.sourceScalar(), viewer)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Could not resolve the relationship source note."));
    try {
      authorizationService.assertAuthorization(viewer, sourceNote);
    } catch (UnexpectedNoAccessRightException e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Could not resolve the relationship source note.");
    }
    String canonicalPropertyKey = PropertyKeyNaming.canonicalExampleOfFamilyKey(propertyKey);
    NoteContentMarkdown.AddPropertyWithAvailableKeyResult addResult =
        NoteContentMarkdown.addPropertyWithAvailableKeyToLeadingFrontmatter(
            sourceNote.getContent(), canonicalPropertyKey, relationship.targetScalar());
    sourceNote.setContent(addResult.content());
    sourceNote.setUpdatedAt(updatedAt);
    entityPersister.merge(sourceNote);
    deleteOrphanImagesForPersistedContent(sourceNote);
    wikiTitleCacheService.refreshForNote(sourceNote, viewer);
    rehomeNoteLevelMemoryTrackerToSourceProperty(
        relationNote, sourceNote, addResult.resolvedKey(), viewer);
  }

  private void rehomeNoteLevelMemoryTrackerToSourceProperty(
      Note relationNote, Note sourceNote, String propertyKey, User viewer) {
    memoryTrackerRepository.findByNote_IdIn(List.of(relationNote.getId())).stream()
        .filter(MemoryTracker::isActive)
        .filter(mt -> mt.getUser().getId().equals(viewer.getId()))
        .filter(mt -> !Boolean.TRUE.equals(mt.getSpelling()))
        .filter(mt -> mt.getPropertyKey() == null || mt.getPropertyKey().isEmpty())
        .findFirst()
        .ifPresent(
            tracker -> {
              tracker.setNote(sourceNote);
              tracker.setPropertyKey(propertyKey);
              entityPersister.merge(tracker);
            });
  }

  private record RelationshipFrontmatter(String sourceScalar, String targetScalar) {}

  private Optional<RelationshipFrontmatter> parseRelationshipFrontmatter(String content) {
    return NoteContentMarkdown.splitLeadingFrontmatter(content == null ? "" : content)
        .flatMap(
            lf -> {
              Frontmatter fm = lf.frontmatter();
              if (!RELATIONSHIP_NOTE_TYPE.equalsIgnoreCase(
                  fm.getString("type").map(String::trim).orElse(""))) {
                return Optional.empty();
              }
              Optional<String> source =
                  fm.getString("source").map(String::trim).filter(s -> !s.isEmpty());
              Optional<String> target =
                  fm.getString("target").map(String::trim).filter(s -> !s.isEmpty());
              if (source.isEmpty() || target.isEmpty()) {
                return Optional.empty();
              }
              return Optional.of(new RelationshipFrontmatter(source.get(), target.get()));
            });
  }

  private Optional<Note> resolveRelationshipSourceNote(
      Note relationNote, String sourceScalar, User viewer) {
    List<String> linkTokens = WikiLinkMarkdown.innerTitlesInOccurrenceOrder(sourceScalar);
    if (linkTokens.isEmpty()) {
      return Optional.empty();
    }
    return wikiLinkResolver.resolveWikiLinkToken(linkTokens.getFirst(), relationNote, viewer);
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
                deleteOrphanImagesForPersistedContent(referrer);
                wikiTitleCacheService.refreshForNote(referrer, viewer);
              });
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
