package com.odde.donut.services;

import com.odde.donut.algorithms.FrontmatterAliases;
import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.algorithms.PathShapedTarget;
import com.odde.donut.algorithms.WikiLinkMarkdown;
import com.odde.donut.algorithms.WikiLinkPropertyMatch;
import com.odde.donut.controllers.dto.FolderTrailSegments;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.NoteAliasIndex;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NoteAliasIndexRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.validators.DisplayNamePathSeparators;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
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

  public record WikiLinkResolution(String authoredLink, Note destinationNote) {}

  public Optional<Note> resolveWikiLinkToken(String token, Note focusNote, User viewer) {
    return Optional.ofNullable(resolveToken(token, viewer, focusNote));
  }

  public Optional<Note> findAccidentalMatch(String answer, Note reviewedNote, User viewer) {
    return findAllAccidentalMatches(answer, reviewedNote, viewer).stream().findFirst();
  }

  public List<Note> findAllAccidentalMatches(String answer, Note reviewedNote, User viewer) {
    if (answer == null || answer.isBlank()) {
      return List.of();
    }
    TreeMap<Integer, Note> matchesById = new TreeMap<>();
    addReadableAccidentalCandidates(
        noteRepository.findByNoteTitleOrderByIdAsc(answer), reviewedNote, viewer, matchesById);
    addReadableAccidentalCandidates(
        aliasAccidentalCandidates(answer), reviewedNote, viewer, matchesById);
    return List.copyOf(matchesById.values());
  }

  private List<Note> aliasAccidentalCandidates(String answer) {
    String trimmed = DisplayNamePathSeparators.trimSurroundingWhitespace(answer);
    if (trimmed == null || trimmed.isBlank()) {
      return List.of();
    }
    String lookupKey = FrontmatterAliases.normalizedLookupKey(trimmed);
    List<Note> notes = new ArrayList<>();
    for (NoteAliasIndex row :
        noteAliasIndexRepository.findByAliasLookupKeyOrderByNoteIdAsc(lookupKey)) {
      notes.add(row.getNote());
    }
    return distinctByNoteId(notes);
  }

  private void addReadableAccidentalCandidates(
      List<Note> candidates, Note reviewedNote, User viewer, TreeMap<Integer, Note> matchesById) {
    for (Note candidate : candidates) {
      Notebook notebook = candidate.getNotebook();
      if (notebook != null
          && authorizationService.userMayReadNotebook(viewer, notebook)
          && !candidate.getId().equals(reviewedNote.getId())) {
        matchesById.putIfAbsent(candidate.getId(), candidate);
      }
    }
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
    List<String> linkTitlesOrdered = NoteContentMarkdown.authoredTokensInOccurrenceOrder(content);
    if (linkTitlesOrdered.isEmpty()) {
      return List.of();
    }
    List<WikiLinkResolution> out = new ArrayList<>();
    for (String token : WikiLinkMarkdown.uniqueAuthoredTokensPreserveOrder(linkTitlesOrdered)) {
      Note target = resolveToken(token, viewer, focusNote);
      if (target != null) {
        out.add(new WikiLinkResolution(token, target));
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
    List<String> linkTitlesOrdered = NoteContentMarkdown.authoredTokensInOccurrenceOrder(content);
    if (linkTitlesOrdered.isEmpty()) {
      return List.of();
    }
    List<String> unresolved = new ArrayList<>();
    for (String token : WikiLinkMarkdown.uniqueAuthoredTokensPreserveOrder(linkTitlesOrdered)) {
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
    Note target =
        WikiLinkMarkdown.splitAuthoredToken(token)
            .portablePath()
            .resolve(focusNotebookName)
            .map(ref -> notebookMatcher.apply(ref.notebookName(), ref.noteTitle()))
            .orElse(null);
    if (target == null
        || !WikiLinkPropertyMatch.matchesTargetNoteContent(token, target.getContent())) {
      return null;
    }
    return target;
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
    return PathShapedTarget.tryParse(noteTitle)
        .map(path -> pathShapedNoteCandidates(notebookName, path))
        .orElseGet(() -> titleOrAliasCandidates(notebookName, noteTitle));
  }

  private List<Note> titleOrAliasCandidates(String notebookName, String noteTitle) {
    List<Note> byTitle =
        noteRepository.findByNotebookNameAndNoteTitleOrderByIdAsc(notebookName, noteTitle);
    List<Note> byAlias = aliasTargetCandidates(notebookName, noteTitle);
    if (byTitle.isEmpty()) {
      return byAlias;
    }
    return uniqueIfExactlyOne(unionByNoteId(byTitle, byAlias));
  }

  private static List<Note> uniqueIfExactlyOne(List<Note> notes) {
    return notes.size() == 1 ? notes : List.of();
  }

  private static List<Note> unionByNoteId(List<Note> first, List<Note> second) {
    List<Note> combined = new ArrayList<>(first);
    combined.addAll(second);
    return distinctByNoteId(combined);
  }

  private static List<Note> distinctByNoteId(List<Note> notes) {
    List<Note> distinct = new ArrayList<>();
    Set<Integer> seenNoteIds = new HashSet<>();
    for (Note note : notes) {
      if (seenNoteIds.add(note.getId())) {
        distinct.add(note);
      }
    }
    return distinct;
  }

  private List<Note> pathShapedNoteCandidates(String notebookName, PathShapedTarget path) {
    List<Note> byTitle =
        noteRepository.findByNotebookNameAndNoteTitleOrderByIdAsc(notebookName, path.title());
    if (byTitle.isEmpty()) {
      return List.of();
    }
    List<Note> inFolder = new ArrayList<>();
    for (Note candidate : byTitle) {
      if (path.matchesTitleAndFolderTrail(
          candidate.getTitle(), FolderTrailSegments.namesFromRootToContainingFolder(candidate))) {
        inFolder.add(candidate);
      }
    }
    return inFolder;
  }

  private List<Note> aliasTargetCandidates(String notebookName, String linkToken) {
    String lookupKey = FrontmatterAliases.normalizedLookupKey(linkToken);
    List<Note> notes = new ArrayList<>();
    for (NoteAliasIndex row :
        noteAliasIndexRepository.findByNotebookNameAndAliasLookupKeyOrderByNoteIdAsc(
            notebookName, lookupKey)) {
      notes.add(row.getNote());
    }
    return distinctByNoteId(notes);
  }
}
