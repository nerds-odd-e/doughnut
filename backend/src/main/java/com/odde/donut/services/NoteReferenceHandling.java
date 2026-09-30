package com.odde.donut.services;

import com.odde.donut.algorithms.Frontmatter;
import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.algorithms.PropertyKeyNaming;
import com.odde.donut.algorithms.RelationshipNoteComposition;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.PropertyFocus;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.AuthoredNoteReferenceInboundFacade;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.validators.AuthoredNoteContent;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reduces a relationship note into a property of its resolved source note, and applies the
 * reference-handling policy, shared by note trash and permanent removal, that removes referrer
 * property links (used for {@link
 * com.odde.donut.controllers.dto.NoteTrashReferenceHandling#REMOVE_FROM_PROPERTIES}).
 */
final class NoteReferenceHandling {
  private static final String RELATIONSHIP_NOTE_TYPE = "relationship";
  // Temporary accommodation for legacy note data: a body that is only "[[A]] words [[B]]." is
  // discarded on reduction like a blank body.
  private static final Pattern LEGACY_RELATIONSHIP_SENTENCE =
      Pattern.compile("\\[\\[[^\\]]+]][^\\[\\]\n]+\\[\\[[^\\]]+]]\\.");

  private final NoteReferenceService noteReferenceService;
  private final WikiLinkResolver wikiLinkResolver;
  private final AuthorizationService authorizationService;
  private final EntityPersister entityPersister;
  private final PropertyMemoryTrackerService propertyMemoryTrackerService;

  NoteReferenceHandling(
      NoteReferenceService noteReferenceService,
      WikiLinkResolver wikiLinkResolver,
      AuthorizationService authorizationService,
      EntityPersister entityPersister,
      PropertyMemoryTrackerService propertyMemoryTrackerService) {
    this.noteReferenceService = noteReferenceService;
    this.wikiLinkResolver = wikiLinkResolver;
    this.authorizationService = authorizationService;
    this.entityPersister = entityPersister;
    this.propertyMemoryTrackerService = propertyMemoryTrackerService;
  }

  /**
   * Parses {@code relationNote}, adds its relationship as a property on the resolved source note,
   * and rehomes every learner's note-level understanding tracker onto that property, regardless of
   * {@code removedFromTracking}. The property key is derived from the note's own {@code relation}
   * frontmatter (hyphens become spaces). Returns the source note.
   */
  Note reduceRelationNoteToSourceProperty(Note relationNote, User viewer, Timestamp updatedAt) {
    RelationshipFrontmatter relationship = relationshipOf(relationNote);
    if (!NoteContentMarkdown.isBodyContentBlank(relationNote.getContent())
        && !isLegacyRelationshipSentence(relationNote.getContent())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "This relationship note has body text and cannot be reduced to a property.");
    }
    String effectivePropertyKey =
        RelationshipNoteComposition.propertyKey(relationship.relationScalar());
    if (effectivePropertyKey == null || effectivePropertyKey.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Property key is required to reduce a relationship note.");
    }
    Note sourceNote = editableRelationshipSource(relationNote, relationship, viewer);
    String canonicalPropertyKey =
        PropertyKeyNaming.canonicalExampleOfFamilyKey(effectivePropertyKey);
    NoteContentMarkdown.AddedPropertyValue added =
        NoteContentMarkdown.addPropertyValueToLeadingFrontmatter(
                sourceNote.getContent(),
                canonicalPropertyKey,
                targetAuthoredFromSourceNotebook(
                    relationship.targetScalar(), relationNote, sourceNote, viewer))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "The source note's \"" + canonicalPropertyKey + "\" cannot take a value."));
    persistReplacedAuthoredContent(sourceNote, added.content(), updatedAt, viewer);
    if (added.formerSingleValue() != null) {
      propertyMemoryTrackerService.followPropertyValue(
          sourceNote, added.key(), added.formerSingleValue());
    }
    propertyMemoryTrackerService.rehomeNoteLevelTrackersToProperty(
        relationNote, sourceNote, new PropertyFocus(added.key(), added.trackedValue()));
    return sourceNote;
  }

  /**
   * The note {@code relationNote}'s {@code source} resolves to for {@code viewer}. Refuses with 400
   * when {@code relationNote} is not a relationship note, or the source is unresolvable or not
   * editable by {@code viewer}.
   */
  Note resolveRelationshipSource(Note relationNote, User viewer) {
    return editableRelationshipSource(relationNote, relationshipOf(relationNote), viewer);
  }

  private RelationshipFrontmatter relationshipOf(Note relationNote) {
    return parseRelationshipFrontmatter(relationNote.getContent())
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "This note is not a relationship note."));
  }

  private Note editableRelationshipSource(
      Note relationNote, RelationshipFrontmatter relationship, User viewer) {
    Note sourceNote =
        wikiLinkResolver
            .resolveFirstWikiLink(relationship.sourceScalar(), relationNote, viewer)
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
    return sourceNote;
  }

  private String targetAuthoredFromSourceNotebook(
      String targetScalar, Note relationNote, Note sourceNote, User viewer) {
    if (sourceNote.getNotebook().getId().equals(relationNote.getNotebook().getId())) {
      return targetScalar;
    }
    return WikiLinkRewriteSupport.markdownLeavingNotebook(
        wikiLinkResolver, targetScalar, relationNote.getNotebook().getName(), viewer);
  }

  private static boolean isLegacyRelationshipSentence(String content) {
    return LEGACY_RELATIONSHIP_SENTENCE
        .matcher(NoteContentMarkdown.bodyWithoutLeadingFrontmatter(content).trim())
        .matches();
  }

  void removeNoteLinksFromReferrerProperties(Note target, User viewer, Timestamp updatedAt) {
    for (AuthoredNoteReferenceInboundFacade.InboundReference inboundReference :
        noteReferenceService.editableInboundReferencesForViewer(target, viewer)) {
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
    noteReferenceService.refreshDerivedIndexesForNote(note);
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
}
