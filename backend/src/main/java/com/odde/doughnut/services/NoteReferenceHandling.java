package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.Frontmatter;
import com.odde.doughnut.algorithms.NoteContentMarkdown;
import com.odde.doughnut.algorithms.PropertyKeyNaming;
import com.odde.doughnut.algorithms.WikiLinkMarkdown;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteWikiTitleCache;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.entities.repositories.NoteWikiTitleCacheRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Applies note-delete reference policies (reduce-to-source / remove-from-properties). */
final class NoteReferenceHandling {
  private static final String RELATIONSHIP_NOTE_TYPE = "relationship";

  private final MemoryTrackerRepository memoryTrackerRepository;
  private final NoteWikiTitleCacheRepository noteWikiTitleCacheRepository;
  private final WikiTitleCacheService wikiTitleCacheService;
  private final WikiLinkResolver wikiLinkResolver;
  private final AuthorizationService authorizationService;
  private final EntityPersister entityPersister;
  private final Consumer<Note> deleteOrphanImages;

  NoteReferenceHandling(
      MemoryTrackerRepository memoryTrackerRepository,
      NoteWikiTitleCacheRepository noteWikiTitleCacheRepository,
      WikiTitleCacheService wikiTitleCacheService,
      WikiLinkResolver wikiLinkResolver,
      AuthorizationService authorizationService,
      EntityPersister entityPersister,
      Consumer<Note> deleteOrphanImages) {
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.noteWikiTitleCacheRepository = noteWikiTitleCacheRepository;
    this.wikiTitleCacheService = wikiTitleCacheService;
    this.wikiLinkResolver = wikiLinkResolver;
    this.authorizationService = authorizationService;
    this.entityPersister = entityPersister;
    this.deleteOrphanImages = deleteOrphanImages;
  }

  void reduceRelationNoteToSourceProperty(
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
    deleteOrphanImages.accept(sourceNote);
    wikiTitleCacheService.refreshForNote(sourceNote, viewer);
    rehomeNoteLevelMemoryTrackerToSourceProperty(
        relationNote, sourceNote, addResult.resolvedKey(), viewer);
  }

  void removeNoteLinksFromReferrerProperties(Note target, User viewer, Timestamp updatedAt) {
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
                deleteOrphanImages.accept(referrer);
                wikiTitleCacheService.refreshForNote(referrer, viewer);
              });
    }
  }

  private void rehomeNoteLevelMemoryTrackerToSourceProperty(
      Note relationNote, Note sourceNote, String propertyKey, User viewer) {
    memoryTrackerRepository.findByNote_IdIn(List.of(relationNote.getId())).stream()
        .filter(MemoryTracker::isActive)
        .filter(mt -> mt.getUser().getId().equals(viewer.getId()))
        .filter(mt -> !mt.isSpelling())
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
    List<String> linkTokens = WikiLinkMarkdown.authoredTokensInOccurrenceOrder(sourceScalar);
    if (linkTokens.isEmpty()) {
      return Optional.empty();
    }
    return wikiLinkResolver.resolveWikiLinkToken(linkTokens.getFirst(), relationNote, viewer);
  }
}
