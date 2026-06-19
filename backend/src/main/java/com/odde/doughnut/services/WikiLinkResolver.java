package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.NoteContentMarkdown;
import com.odde.doughnut.algorithms.WikiLinkMarkdown;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

@Service
public class WikiLinkResolver {

  private final NoteRepository noteRepository;
  private final AuthorizationService authorizationService;

  public WikiLinkResolver(
      NoteRepository noteRepository, AuthorizationService authorizationService) {
    this.noteRepository = noteRepository;
    this.authorizationService = authorizationService;
  }

  public record ResolvedWikiLink(String linkText, Note targetNote) {}

  public Optional<Note> resolveWikiLinkToken(String token, Note focusNote, User viewer) {
    return Optional.ofNullable(resolveToken(token, viewer, focusNote));
  }

  /** Resolves a wiki-link token to any matching note, regardless of viewer readability. */
  public Optional<Note> resolveAnyTargetWikiLinkToken(String token, Note focusNote) {
    return Optional.ofNullable(resolveAnyTargetToken(token, focusNote));
  }

  public List<ResolvedWikiLink> resolveWikiLinksForCache(Note focusNote, User viewer) {
    String content = focusNote.getContent();
    if (content == null || content.isBlank()) {
      return List.of();
    }
    List<String> linkTitlesOrdered = NoteContentMarkdown.wikiLinkInnersInOccurrenceOrder(content);
    if (linkTitlesOrdered.isEmpty()) {
      return List.of();
    }
    List<ResolvedWikiLink> out = new ArrayList<>();
    for (String token : dedupePreserveOrder(linkTitlesOrdered)) {
      Note target = resolveToken(token, viewer, focusNote);
      if (target != null) {
        out.add(new ResolvedWikiLink(token, target));
      }
    }
    return List.copyOf(out);
  }

  private Note resolveAnyTargetToken(String token, Note focusNote) {
    return resolveParsedLink(token, focusNote, this::firstNotebookMatch);
  }

  private Note resolveToken(String token, User viewer, Note focusNote) {
    return resolveParsedLink(
        token,
        focusNote,
        (notebookName, noteTitle) -> firstReadableNotebookMatch(notebookName, noteTitle, viewer));
  }

  private Note resolveParsedLink(
      String token, Note focusNote, BiFunction<String, String, Note> notebookMatcher) {
    var parts = WikiLinkMarkdown.splitInner(token);
    String resolutionKey = parts.target();
    if (resolutionKey == null || resolutionKey.isBlank()) {
      return null;
    }
    Qualified qualified = Qualified.tryParse(resolutionKey);
    if (qualified != null) {
      return notebookMatcher.apply(qualified.notebookName(), qualified.noteTitle());
    }
    Notebook focusNotebook = focusNote.getNotebook();
    if (focusNotebook == null) {
      return null;
    }
    return notebookMatcher.apply(focusNotebook.getName(), resolutionKey);
  }

  private Note firstNotebookMatch(String notebookName, String noteTitle) {
    List<Note> candidates = noteCandidates(notebookName, noteTitle);
    return candidates.isEmpty() ? null : candidates.getFirst();
  }

  private Note firstReadableNotebookMatch(String notebookName, String noteTitle, User viewer) {
    for (Note candidate : noteCandidates(notebookName, noteTitle)) {
      Notebook notebook = candidate.getNotebook();
      if (notebook != null && authorizationService.userMayReadNotebook(viewer, notebook)) {
        return candidate;
      }
    }
    return null;
  }

  private List<Note> noteCandidates(String notebookName, String noteTitle) {
    return noteRepository.findByNotebookNameAndNoteTitleOrderByIdAsc(notebookName, noteTitle);
  }

  private record Qualified(String notebookName, String noteTitle) {
    static Qualified tryParse(String token) {
      int i = token.indexOf(':');
      if (i <= 0 || i >= token.length() - 1) {
        return null;
      }
      String nb = token.substring(0, i).trim();
      String nt = token.substring(i + 1).trim();
      if (nb.isEmpty() || nt.isEmpty()) {
        return null;
      }
      return new Qualified(nb, nt);
    }
  }

  private static List<String> dedupePreserveOrder(List<String> titles) {
    List<String> out = new ArrayList<>();
    Set<String> seenNormalized = new HashSet<>();
    for (String t : titles) {
      String key = Normalizer.normalize(t, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
      if (seenNormalized.add(key)) {
        out.add(t);
      }
    }
    return out;
  }
}
