package com.odde.donut.services;

import com.odde.donut.algorithms.AuthoredNoteReference;
import com.odde.donut.algorithms.AuthoredNoteReferences;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.algorithms.NoteReferenceResolution;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NoteAliasIndexRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class WikiLinkResolver {

  private final NoteRepository noteRepository;
  private final AuthorizationService authorizationService;
  private final AccidentalWikiLinkMatches accidentalWikiLinkMatches;
  private final WikiLinkCandidateClassifier candidateClassifier;
  private final CanonicalDonutOrigin canonicalDonutOrigin;

  public WikiLinkResolver(
      NoteRepository noteRepository,
      NoteAliasIndexRepository noteAliasIndexRepository,
      AuthorizationService authorizationService,
      CanonicalDonutOrigin canonicalDonutOrigin) {
    this.noteRepository = noteRepository;
    this.authorizationService = authorizationService;
    this.canonicalDonutOrigin = canonicalDonutOrigin;
    this.candidateClassifier =
        new WikiLinkCandidateClassifier(
            new WikiLinkNoteCandidates(noteRepository, noteAliasIndexRepository),
            authorizationService);
    this.accidentalWikiLinkMatches =
        new AccidentalWikiLinkMatches(
            noteRepository, noteAliasIndexRepository, authorizationService);
  }

  CanonicalDonutOrigin canonicalDonutOrigin() {
    return canonicalDonutOrigin;
  }

  public record WikiLinkResolution(String authoredLink, Note destinationNote) {}

  /**
   * Cardinality of a wiki Portable-path token's readable candidates in a notebook scope: exactly
   * one match ({@link Resolved}), no match ({@link Unresolved}), or more than one ({@link
   * Ambiguous}).
   */
  public sealed interface CandidateCardinality {
    record Resolved(Note destinationNote) implements CandidateCardinality {}

    record Unresolved() implements CandidateCardinality {}

    record Ambiguous() implements CandidateCardinality {}
  }

  /**
   * The one domain-stable resolution entry point for an {@link AuthoredNoteReference}: resolves
   * either a wiki Portable-path target (reusing {@link #classifyToken}) or a note-ID URL target
   * (looked up by ID, excluding soft-deleted notes, with the same viewer-readability check applied
   * to note-ID URL targets elsewhere, e.g. {@code ResolvedWikiLinkService}) against {@code
   * sourceNote}'s scope and {@code viewer}'s current readability.
   */
  public NoteReferenceResolution resolveReference(
      AuthoredNoteReference ref, Note sourceNote, User viewer) {
    return switch (ref) {
      case AuthoredNoteReference.WikiPortablePathTarget wiki ->
          toResolution(classifyToken(wiki.authoredLink(), sourceNote, viewer));
      case AuthoredNoteReference.NoteIdUrlTarget url ->
          resolveNoteIdUrlTarget(url, sourceNote, viewer);
    };
  }

  private static NoteReferenceResolution toResolution(CandidateCardinality cardinality) {
    return switch (cardinality) {
      case CandidateCardinality.Resolved resolved ->
          new NoteReferenceResolution.Resolved(resolved.destinationNote());
      case CandidateCardinality.Unresolved ignored -> new NoteReferenceResolution.Missing();
      case CandidateCardinality.Ambiguous ignored -> new NoteReferenceResolution.Ambiguous();
    };
  }

  private NoteReferenceResolution resolveNoteIdUrlTarget(
      AuthoredNoteReference.NoteIdUrlTarget url, Note sourceNote, User viewer) {
    Note target = noteRepository.findById(url.noteId()).orElse(null);
    if (target == null || target.getDeletedAt() != null) {
      return new NoteReferenceResolution.Missing();
    }
    Notebook notebook =
        target.getNotebook() != null ? target.getNotebook() : sourceNote.getNotebook();
    if (!authorizationService.userMayReadNotebook(viewer, notebook)) {
      return new NoteReferenceResolution.Missing();
    }
    return new NoteReferenceResolution.Resolved(target);
  }

  public Optional<Note> resolveWikiLinkToken(String token, Note focusNote, User viewer) {
    return switch (classifyToken(token, focusNote, viewer)) {
      case CandidateCardinality.Resolved resolved -> Optional.of(resolved.destinationNote());
      case CandidateCardinality.Unresolved ignored -> Optional.empty();
      case CandidateCardinality.Ambiguous ignored -> Optional.empty();
    };
  }

  public Optional<Note> findAccidentalMatch(String answer, Note reviewedNote, User viewer) {
    return findAllAccidentalMatches(answer, reviewedNote, viewer).stream().findFirst();
  }

  public List<Note> findAllAccidentalMatches(String answer, Note reviewedNote, User viewer) {
    return accidentalWikiLinkMatches.findAll(answer, reviewedNote, viewer);
  }

  public List<WikiLinkResolution> resolveWikiLinksForCache(Note focusNote, User viewer) {
    String content = focusNote.getContent();
    if (content == null || content.isBlank()) {
      return List.of();
    }
    List<WikiLinkResolution> out = new ArrayList<>();
    for (AuthoredNoteReference ref :
        AuthoredNoteReferences.uniquePreserveOrder(
            AuthoredNoteReferences.inOccurrenceOrder(content, canonicalDonutOrigin))) {
      switch (ref) {
        case AuthoredNoteReference.WikiPortablePathTarget wiki -> {
          if (classifyToken(wiki.authoredLink(), focusNote, viewer)
              instanceof CandidateCardinality.Resolved resolved) {
            out.add(new WikiLinkResolution(wiki.authoredLink(), resolved.destinationNote()));
          }
        }
        case AuthoredNoteReference.NoteIdUrlTarget url -> {
          Note target = noteRepository.findById(url.noteId()).orElse(null);
          if (target != null && target.getDeletedAt() == null) {
            out.add(new WikiLinkResolution(url.authoredLink(), target));
          }
        }
      }
    }
    return List.copyOf(out);
  }

  /**
   * Missing wiki-link inners for the viewer, in first-occurrence order (same extract/dedupe/resolve
   * as cache). A token with several readable matches is ambiguous, not missing, and is excluded.
   * Note-ID URL references are not wiki tokens and are excluded.
   */
  public List<String> missingWikiLinkTokens(Note focusNote, User viewer) {
    String content = focusNote.getContent();
    if (content == null || content.isBlank()) {
      return List.of();
    }
    List<String> missing = new ArrayList<>();
    for (AuthoredNoteReference.WikiPortablePathTarget wiki :
        AuthoredNoteReferences.uniqueWikiPortablePathTargets(content)) {
      String token = wiki.authoredLink();
      if (classifyToken(token, focusNote, viewer) instanceof CandidateCardinality.Unresolved) {
        missing.add(token);
      }
    }
    return List.copyOf(missing);
  }

  /**
   * Classifies {@code token}'s readable-candidate cardinality for {@code viewer} in {@code
   * focusNote}'s notebook scope (with Portable-path notebook fallback).
   */
  public CandidateCardinality classifyToken(String token, Note focusNote, User viewer) {
    String focusNotebookName =
        focusNote.getNotebook() == null ? null : focusNote.getNotebook().getName();
    return classifyToken(token, focusNotebookName, viewer);
  }

  /**
   * Classifies {@code token}'s readable-candidate cardinality for {@code viewer} resolved against
   * {@code notebookFallbackName}. Used to classify a token in a notebook scope other than its
   * current note's (e.g. before a cross-notebook move rewrites content).
   */
  CandidateCardinality classifyToken(String token, String notebookFallbackName, User viewer) {
    return candidateClassifier.classify(token, notebookFallbackName, viewer);
  }

  /**
   * True when, among the viewer's readable candidates for this notebook/title combination, the
   * given note is the sole match.
   */
  public boolean readableNotebookMatchUniquelyIdentifies(
      String notebookName, String noteTitle, User viewer, Note note) {
    return candidateClassifier.readableNotebookMatchUniquelyIdentifies(
        notebookName, noteTitle, viewer, note);
  }
}
