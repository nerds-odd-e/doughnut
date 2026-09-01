package com.odde.donut.services;

import com.odde.donut.algorithms.AuthoredNoteReference;
import com.odde.donut.algorithms.AuthoredNoteReferences;
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

  public WikiLinkResolver(
      NoteRepository noteRepository,
      NoteAliasIndexRepository noteAliasIndexRepository,
      AuthorizationService authorizationService) {
    this.noteRepository = noteRepository;
    this.authorizationService = authorizationService;
    this.noteCandidates = new WikiLinkNoteCandidates(noteRepository, noteAliasIndexRepository);
    this.accidentalWikiLinkMatches =
        new AccidentalWikiLinkMatches(
            noteRepository, noteAliasIndexRepository, authorizationService);
  }

  public record WikiLinkResolution(String authoredLink, Note destinationNote) {}

  public Optional<Note> resolveWikiLinkToken(String token, Note focusNote, User viewer) {
    return Optional.ofNullable(resolveToken(token, viewer, focusNote));
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
            AuthoredNoteReferences.inOccurrenceOrder(content))) {
      switch (ref) {
        case AuthoredNoteReference.WikiPortablePathTarget wiki -> {
          Note target = resolveToken(wiki.authoredLink(), viewer, focusNote);
          if (target != null) {
            out.add(new WikiLinkResolution(wiki.authoredLink(), target));
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
      if (resolveToken(token, viewer, focusNote) == null
          && !isAmbiguousToken(token, focusNote, viewer)) {
        missing.add(token);
      }
    }
    return List.copyOf(missing);
  }

  /** True when, among the viewer's readable candidates for this token, more than one matches. */
  boolean isAmbiguousToken(String token, Note focusNote, User viewer) {
    String focusNotebookName =
        focusNote.getNotebook() == null ? null : focusNote.getNotebook().getName();
    return isAmbiguousToken(token, focusNotebookName, viewer);
  }

  /**
   * True when, among the viewer's readable candidates for this token resolved against {@code
   * notebookFallbackName}, more than one matches. Used to check a token's ambiguity in a notebook
   * scope other than its current note's (e.g. before a cross-notebook move rewrites content).
   */
  boolean isAmbiguousToken(String token, String notebookFallbackName, User viewer) {
    return resolveRef(token, notebookFallbackName)
        .map(ref -> readableNotebookMatches(ref.notebookName(), ref.noteTitle(), viewer).size() > 1)
        .orElse(false);
  }

  private Note resolveAnyTargetToken(String token, Note focusNote) {
    return resolveParsedLink(token, focusNote, this::uniqueNotebookMatch);
  }

  private Note resolveToken(String token, User viewer, Note focusNote) {
    return resolveParsedLink(
        token,
        focusNote,
        (notebookName, noteTitle) -> uniqueReadableNotebookMatch(notebookName, noteTitle, viewer));
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
