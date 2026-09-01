package com.odde.donut.services;

import com.odde.donut.algorithms.AuthoredNoteReference;
import com.odde.donut.algorithms.AuthoredNoteReferences;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.algorithms.PortablePath;
import com.odde.donut.algorithms.WikiLinkMarkdown;
import com.odde.donut.algorithms.WikiLinkPropertyMatch;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NoteAliasIndexRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

@Service
public class WikiLinkResolver {

  private final NoteRepository noteRepository;
  private final AuthorizationService authorizationService;
  private final AccidentalWikiLinkMatches accidentalWikiLinkMatches;
  private final WikiLinkNoteCandidates noteCandidates;
  private final CanonicalDonutOrigin canonicalDonutOrigin;

  public WikiLinkResolver(
      NoteRepository noteRepository,
      NoteAliasIndexRepository noteAliasIndexRepository,
      AuthorizationService authorizationService,
      CanonicalDonutOrigin canonicalDonutOrigin) {
    this.noteRepository = noteRepository;
    this.authorizationService = authorizationService;
    this.canonicalDonutOrigin = canonicalDonutOrigin;
    this.noteCandidates = new WikiLinkNoteCandidates(noteRepository, noteAliasIndexRepository);
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

  /** Resolves a wiki-link token to any matching note, regardless of viewer readability. */
  public Optional<Note> resolveAnyTargetWikiLinkToken(String token, Note focusNote) {
    return Optional.ofNullable(resolveAnyTargetToken(token, focusNote));
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
    return resolveRef(token, notebookFallbackName)
        .map(
            ref ->
                classifyCandidates(
                    token, readableNotebookMatches(ref.notebookName(), ref.noteTitle(), viewer)))
        .orElseGet(CandidateCardinality.Unresolved::new);
  }

  private static CandidateCardinality classifyCandidates(String token, List<Note> readable) {
    if (readable.size() > 1) {
      return new CandidateCardinality.Ambiguous();
    }
    if (readable.size() == 1) {
      Note candidate = readable.getFirst();
      if (WikiLinkPropertyMatch.matchesTargetNoteContent(token, candidate.getContent())) {
        return new CandidateCardinality.Resolved(candidate);
      }
    }
    return new CandidateCardinality.Unresolved();
  }

  private Note resolveAnyTargetToken(String token, Note focusNote) {
    return resolveParsedLink(token, focusNote, this::uniqueNotebookMatch);
  }

  private Note resolveParsedLink(
      String token, Note focusNote, BiFunction<String, String, Note> notebookMatcher) {
    Note target =
        resolveRef(token, focusNote)
            .map(ref -> notebookMatcher.apply(ref.notebookName(), ref.noteTitle()))
            .orElse(null);
    if (target == null
        || !WikiLinkPropertyMatch.matchesTargetNoteContent(token, target.getContent())) {
      return null;
    }
    return target;
  }

  /** Parses {@code token} into a notebook/title ref, applying the focus-notebook fallback. */
  private Optional<PortablePath.Resolved> resolveRef(String token, Note focusNote) {
    String focusNotebookName =
        focusNote.getNotebook() == null ? null : focusNote.getNotebook().getName();
    return resolveRef(token, focusNotebookName);
  }

  /** Parses {@code token} into a notebook/title ref, applying the given notebook-name fallback. */
  private Optional<PortablePath.Resolved> resolveRef(String token, String notebookFallbackName) {
    return WikiLinkMarkdown.splitInner(token).portablePath().resolve(notebookFallbackName);
  }

  private Note uniqueNotebookMatch(String notebookName, String noteTitle) {
    return uniqueIfExactlyOne(noteCandidates.forNotebookAndTitle(notebookName, noteTitle));
  }

  private Note uniqueReadableNotebookMatch(String notebookName, String noteTitle, User viewer) {
    return uniqueIfExactlyOne(readableNotebookMatches(notebookName, noteTitle, viewer));
  }

  /**
   * True when, among the viewer's readable candidates for this notebook/title combination, the
   * given note is the sole match.
   */
  public boolean readableNotebookMatchUniquelyIdentifies(
      String notebookName, String noteTitle, User viewer, Note note) {
    Note match = uniqueReadableNotebookMatch(notebookName, noteTitle, viewer);
    return match != null && match.getId().equals(note.getId());
  }

  List<Note> readableNotebookMatches(String notebookName, String noteTitle, User viewer) {
    List<Note> readable = new ArrayList<>();
    for (Note candidate : noteCandidates.forNotebookAndTitle(notebookName, noteTitle)) {
      Notebook notebook = candidate.getNotebook();
      if (notebook != null && authorizationService.userMayReadNotebook(viewer, notebook)) {
        readable.add(candidate);
      }
    }
    return readable;
  }

  private static Note uniqueIfExactlyOne(List<Note> notes) {
    return notes.size() == 1 ? notes.getFirst() : null;
  }
}
