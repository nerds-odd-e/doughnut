package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.FrontmatterAliases;
import com.odde.doughnut.algorithms.NoteContentMarkdown;
import com.odde.doughnut.algorithms.WikiLinkTargetReference;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteAliasIndex;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteAliasIndexRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

@Service
public class WikiLinkResolver {

  private final NoteRepository noteRepository;
  private final NoteAliasIndexRepository noteAliasIndexRepository;
  private final AuthorizationService authorizationService;

  public WikiLinkResolver(
      NoteRepository noteRepository,
      NoteAliasIndexRepository noteAliasIndexRepository,
      AuthorizationService authorizationService) {
    this.noteRepository = noteRepository;
    this.noteAliasIndexRepository = noteAliasIndexRepository;
    this.authorizationService = authorizationService;
  }

  public record ResolvedWikiLink(String linkText, Note targetNote) {}

  public Optional<Note> resolveWikiLinkToken(String token, Note focusNote, User viewer) {
    return Optional.ofNullable(resolveToken(token, viewer, focusNote));
  }

  public Optional<Note> findAccidentalMatch(String answer, Note reviewedNote, User viewer) {
    for (Note candidate : noteRepository.findByNoteTitleOrderByIdAsc(answer)) {
      Notebook notebook = candidate.getNotebook();
      if (notebook != null
          && authorizationService.userMayReadNotebook(viewer, notebook)
          && !candidate.getId().equals(reviewedNote.getId())) {
        return Optional.of(candidate);
      }
    }
    return Optional.empty();
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

  /**
   * Unresolved wiki-link inners for the viewer, in first-occurrence order (same
   * extract/dedupe/resolve as cache).
   */
  public List<String> unresolvedWikiLinkTokens(Note focusNote, User viewer) {
    String content = focusNote.getContent();
    if (content == null || content.isBlank()) {
      return List.of();
    }
    List<String> linkTitlesOrdered = NoteContentMarkdown.wikiLinkInnersInOccurrenceOrder(content);
    if (linkTitlesOrdered.isEmpty()) {
      return List.of();
    }
    List<String> unresolved = new ArrayList<>();
    for (String token : dedupePreserveOrder(linkTitlesOrdered)) {
      if (resolveToken(token, viewer, focusNote) == null) {
        unresolved.add(token);
      }
    }
    return List.copyOf(unresolved);
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
    String focusNotebookName =
        focusNote.getNotebook() == null ? null : focusNote.getNotebook().getName();
    return WikiLinkTargetReference.forToken(token, focusNotebookName)
        .map(ref -> notebookMatcher.apply(ref.notebookName(), ref.noteTitle()))
        .orElse(null);
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
    List<Note> byTitle =
        noteRepository.findByNotebookNameAndNoteTitleOrderByIdAsc(notebookName, noteTitle);
    if (!byTitle.isEmpty()) {
      return byTitle;
    }
    return aliasTargetCandidates(notebookName, noteTitle);
  }

  private List<Note> aliasTargetCandidates(String notebookName, String linkToken) {
    String lookupKey = FrontmatterAliases.normalizedLookupKey(linkToken);
    List<NoteAliasIndex> rows =
        noteAliasIndexRepository.findByNotebookNameAndAliasLookupKeyOrderByNoteIdAsc(
            notebookName, lookupKey);
    if (rows.isEmpty()) {
      return List.of();
    }
    List<Note> distinctNotes = new ArrayList<>();
    Set<Integer> seenNoteIds = new HashSet<>();
    for (NoteAliasIndex row : rows) {
      Note note = row.getNote();
      if (seenNoteIds.add(note.getId())) {
        distinctNotes.add(note);
      }
    }
    return distinctNotes;
  }

  private static List<String> dedupePreserveOrder(List<String> titles) {
    List<String> out = new ArrayList<>();
    Set<String> seenNormalized = new HashSet<>();
    for (String t : titles) {
      if (seenNormalized.add(FrontmatterAliases.normalizedLookupKey(t))) {
        out.add(t);
      }
    }
    return out;
  }
}
