package com.odde.donut.services;

import com.odde.donut.algorithms.Frontmatter;
import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.algorithms.PropertyKeyNaming;
import com.odde.donut.algorithms.WikiLinkMarkdown;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.ResolvedWikiLink;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.entities.repositories.ResolvedWikiLinkRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.validators.AuthoredNoteContent;
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
  private final ResolvedWikiLinkRepository resolvedWikiLinkRepository;
  private final ResolvedWikiLinkService resolvedWikiLinkService;
  private final WikiLinkResolver wikiLinkResolver;
  private final AuthorizationService authorizationService;
  private final EntityPersister entityPersister;
  private final Consumer<Note> deleteOrphanImages;

  NoteReferenceHandling(
      MemoryTrackerRepository memoryTrackerRepository,
      ResolvedWikiLinkRepository resolvedWikiLinkRepository,
      ResolvedWikiLinkService resolvedWikiLinkService,
      WikiLinkResolver wikiLinkResolver,
      AuthorizationService authorizationService,
      EntityPersister entityPersister,
      Consumer<Note> deleteOrphanImages) {
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.resolvedWikiLinkRepository = resolvedWikiLinkRepository;
    this.resolvedWikiLinkService = resolvedWikiLinkService;
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
    persistReplacedAuthoredContent(sourceNote, addResult.content(), updatedAt, viewer);
    rehomeNoteLevelMemoryTrackerToSourceProperty(
        relationNote, sourceNote, addResult.resolvedKey(), viewer);
  }

  void removeNoteLinksFromReferrerProperties(Note target, User viewer, Timestamp updatedAt) {
    Map<Note, Set<String>> referrersByLinkTexts = new LinkedHashMap<>();
    for (ResolvedWikiLink row :
        resolvedWikiLinkRepository.findRowsReferringToNonDeletedNotesForTarget(target.getId())) {
      referrersByLinkTexts
          .computeIfAbsent(row.getSourceNote(), ignored -> new LinkedHashSet<>())
          .add(row.getAuthoredLink());
    }
    for (Map.Entry<Note, Set<String>> entry : referrersByLinkTexts.entrySet()) {
      Note referrer = entry.getKey();
      NoteContentMarkdown.removeWikiLinksFromLeadingFrontmatterProperties(
              referrer.getContent(), entry.getValue())
          .ifPresent(
              updatedContent ->
                  persistReplacedAuthoredContent(referrer, updatedContent, updatedAt, viewer));
    }
  }

  private void persistReplacedAuthoredContent(
      Note note, String markdown, Timestamp updatedAt, User viewer) {
    note.replaceContent(
        AuthoredNoteContent.prepareDocumentForSave(
            markdown, wikiLinkResolver.canonicalDonutOrigin()));
    note.setUpdatedAt(updatedAt);
    entityPersister.merge(note);
    deleteOrphanImages.accept(note);
    resolvedWikiLinkService.refreshForNote(note, viewer);
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
