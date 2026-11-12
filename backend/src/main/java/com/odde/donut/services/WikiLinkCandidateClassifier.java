package com.odde.donut.services;

import com.odde.donut.algorithms.PortablePath;
import com.odde.donut.algorithms.WikiLinkMarkdown;
import com.odde.donut.algorithms.WikiLinkPropertyMatch;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import java.util.List;
import java.util.Optional;

/**
 * Readable-candidate cardinality classification of a wiki Portable-path token against
 * notebook-scoped note candidates. Backs {@link WikiLinkResolver}'s public token-resolution
 * methods.
 */
final class WikiLinkCandidateClassifier {

  private final WikiLinkNoteCandidates noteCandidates;
  private final AuthorizationService authorizationService;

  WikiLinkCandidateClassifier(
      WikiLinkNoteCandidates noteCandidates, AuthorizationService authorizationService) {
    this.noteCandidates = noteCandidates;
    this.authorizationService = authorizationService;
  }

  /**
   * Classifies {@code token}'s readable-candidate cardinality for {@code viewer} resolved against
   * {@code notebookFallbackName}.
   */
  WikiLinkResolver.CandidateCardinality classify(
      String token, String notebookFallbackName, User viewer) {
    List<Note> candidates =
        resolveRef(token, notebookFallbackName)
            .map(ref -> readableNotebookMatches(ref.notebookName(), ref.noteTitle(), viewer))
            .orElse(List.of());
    return classifyCandidates(token, candidates);
  }

  private static WikiLinkResolver.CandidateCardinality classifyCandidates(
      String token, List<Note> readable) {
    if (readable.size() > 1) {
      return new WikiLinkResolver.CandidateCardinality.Ambiguous(readable);
    }
    if (readable.size() == 1) {
      Note candidate = readable.getFirst();
      if (WikiLinkPropertyMatch.matchesTargetNoteContent(token, candidate.getContent())) {
        return new WikiLinkResolver.CandidateCardinality.Resolved(candidate);
      }
    }
    return new WikiLinkResolver.CandidateCardinality.Unresolved();
  }

  /** Parses {@code token} into a notebook/title ref, applying the given notebook-name fallback. */
  private Optional<PortablePath.Resolved> resolveRef(String token, String notebookFallbackName) {
    return WikiLinkMarkdown.splitInner(token).portablePath().resolve(notebookFallbackName);
  }

  private Note uniqueReadableNotebookMatch(String notebookName, String noteTitle, User viewer) {
    return uniqueIfExactlyOne(readableNotebookMatches(notebookName, noteTitle, viewer));
  }

  /**
   * True when, among the viewer's readable candidates for this notebook/title combination, the
   * given note is the sole match.
   */
  boolean readableNotebookMatchUniquelyIdentifies(
      String notebookName, String noteTitle, User viewer, Note note) {
    Note match = uniqueReadableNotebookMatch(notebookName, noteTitle, viewer);
    return match != null && match.getId().equals(note.getId());
  }

  List<Note> readableNotebookMatches(String notebookName, String noteTitle, User viewer) {
    return noteCandidates.forNotebookAndTitle(
        notebookName,
        noteTitle,
        note ->
            note.getNotebook() != null
                && authorizationService.userMayReadNotebook(viewer, note.getNotebook()));
  }

  private static Note uniqueIfExactlyOne(List<Note> notes) {
    return notes.size() == 1 ? notes.getFirst() : null;
  }
}
