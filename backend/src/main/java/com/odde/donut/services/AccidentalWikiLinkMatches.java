package com.odde.donut.services;

import com.odde.donut.algorithms.FrontmatterAliases;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.NoteAliasIndex;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NoteAliasIndexRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.validators.DisplayNamePathSeparators;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Notes whose title or alias happens to match a spelling-recall answer, distinct from wiki-link
 * resolution proper. Used by {@code SpellingRecallGrading} to flag an answer that accidentally
 * names another note.
 */
final class AccidentalWikiLinkMatches {

  private final NoteRepository noteRepository;
  private final NoteAliasIndexRepository noteAliasIndexRepository;
  private final AuthorizationService authorizationService;

  AccidentalWikiLinkMatches(
      NoteRepository noteRepository,
      NoteAliasIndexRepository noteAliasIndexRepository,
      AuthorizationService authorizationService) {
    this.noteRepository = noteRepository;
    this.noteAliasIndexRepository = noteAliasIndexRepository;
    this.authorizationService = authorizationService;
  }

  List<Note> findAll(String answer, Note reviewedNote, User viewer) {
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
    return WikiLinkResolver.distinctByNoteId(notes);
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
}
