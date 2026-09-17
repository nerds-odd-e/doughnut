package com.odde.donut.services;

import com.odde.donut.algorithms.Frontmatter;
import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.algorithms.PropertyKeyNaming;
import com.odde.donut.algorithms.WikiLinkMarkdown;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.AuthoredNoteReferenceInboundFacade;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.validators.AuthoredNoteContent;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Applies note-delete reference policies (reduce-to-source / remove-from-properties). */
final class NoteReferenceHandling {
  private static final String RELATIONSHIP_NOTE_TYPE = "relationship";

  private final MemoryTrackerRepository memoryTrackerRepository;
  private final NoteReferenceService noteReferenceService;
  private final WikiLinkResolver wikiLinkResolver;
  private final AuthorizationService authorizationService;
  private final EntityPersister entityPersister;
  private final Consumer<Note> deleteOrphanImages;

  NoteReferenceHandling(
      MemoryTrackerRepository memoryTrackerRepository,
      NoteReferenceService noteReferenceService,
      WikiLinkResolver wikiLinkResolver,
      AuthorizationService authorizationService,
      EntityPersister entityPersister,
      Consumer<Note> deleteOrphanImages) {
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.noteReferenceService = noteReferenceService;
    this.wikiLinkResolver = wikiLinkResolver;
    this.authorizationService = authorizationService;
    this.entityPersister = entityPersister;
    this.deleteOrphanImages = deleteOrphanImages;
  }

  /**
   * Parses {@code relationNote}, adds its relationship as a property on the resolved source note,
   * and rehomes every learner's note-level understanding tracker onto that property, regardless of
   * {@code removedFromTracking}. {@code propertyKey}, when supplied, is used as-is (the trash
   * reduce contract's client-computed label); otherwise the key is derived from the note's own
   * {@code relation} frontmatter (hyphens become spaces). Returns the source note.
   */
  Note reduceRelationNoteToSourceProperty(
      Note relationNote, String propertyKey, User viewer, Timestamp updatedAt) {
    RelationshipFrontmatter relationship =
        parseRelationshipFrontmatter(relationNote.getContent())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "This note is not a relationship note."));
    String effectivePropertyKey =
        propertyKey == null || propertyKey.isBlank()
            ? propertyKeyFromRelationScalar(relationship.relationScalar())
            : propertyKey;
    if (effectivePropertyKey == null || effectivePropertyKey.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Property key is required to reduce a relationship note.");
    }
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
    String canonicalPropertyKey =
        PropertyKeyNaming.canonicalExampleOfFamilyKey(effectivePropertyKey);
    NoteContentMarkdown.AddPropertyWithAvailableKeyResult addResult =
        NoteContentMarkdown.addPropertyWithAvailableKeyToLeadingFrontmatter(
            sourceNote.getContent(), canonicalPropertyKey, relationship.targetScalar());
    persistReplacedAuthoredContent(sourceNote, addResult.content(), updatedAt, viewer);
    rehomeNoteLevelMemoryTrackerToSourceProperty(relationNote, sourceNote, addResult.resolvedKey());
    return sourceNote;
  }

  /** Same rule as frontend {@code relationTypeFromKebab}: hyphens become spaces, trimmed. */
  private static String propertyKeyFromRelationScalar(String relationScalar) {
    if (relationScalar == null) {
      return null;
    }
    String derived = relationScalar.replace('-', ' ').trim();
    return derived.isEmpty() ? null : derived;
  }

  void removeNoteLinksFromReferrerProperties(Note target, User viewer, Timestamp updatedAt) {
    for (AuthoredNoteReferenceInboundFacade.InboundReference inboundReference :
        noteReferenceService.distinctInboundReferencesForViewer(target, viewer)) {
      Note referrer = inboundReference.referrer();
      NoteContentMarkdown.removeWikiLinksFromLeadingFrontmatterProperties(
              referrer.getContent(), Set.copyOf(inboundReference.authoredLinkTexts()))
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
    noteReferenceService.refreshDerivedIndexesForNote(note);
  }

  /**
   * Moves every learner's note-level understanding tracker onto the source property. Every other
   * tracker on {@code relationNote} (spelling, commissioned, or property-level) is left for the DB
   * {@code ON DELETE CASCADE} to remove with the relationship note; those are detached here so
   * Hibernate's persistence context does not keep a managed reference to a note about to be removed
   * (its own pre-flush transient-dependency check does not know about that DB-level cascade).
   */
  private void rehomeNoteLevelMemoryTrackerToSourceProperty(
      Note relationNote, Note sourceNote, String propertyKey) {
    memoryTrackerRepository
        .findByNote_IdIn(List.of(relationNote.getId()))
        .forEach(
            tracker -> {
              if (tracker.isUnderstanding() && tracker.isNoteLevelTracker()) {
                tracker.setNote(sourceNote);
                tracker.setPropertyKey(propertyKey);
                entityPersister.merge(tracker);
              } else {
                entityPersister.detach(tracker);
              }
            });
  }

  private record RelationshipFrontmatter(
      String relationScalar, String sourceScalar, String targetScalar) {}

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
              String relation = fm.getString("relation").map(String::trim).orElse(null);
              return Optional.of(new RelationshipFrontmatter(relation, source.get(), target.get()));
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
