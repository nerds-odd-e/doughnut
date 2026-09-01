package com.odde.donut.services;

import com.odde.donut.algorithms.FrontmatterAliases;
import com.odde.donut.algorithms.PathShapedTarget;
import com.odde.donut.controllers.dto.FolderTrailSegments;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.NoteAliasIndex;
import com.odde.donut.entities.repositories.NoteAliasIndexRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Notebook-scoped note candidates for wiki-link title, path-shaped, and alias matching. */
final class WikiLinkNoteCandidates {

  private final NoteRepository noteRepository;
  private final NoteAliasIndexRepository noteAliasIndexRepository;

  WikiLinkNoteCandidates(
      NoteRepository noteRepository, NoteAliasIndexRepository noteAliasIndexRepository) {
    this.noteRepository = noteRepository;
    this.noteAliasIndexRepository = noteAliasIndexRepository;
  }

  List<Note> forNotebookAndTitle(String notebookName, String noteTitle) {
    return PathShapedTarget.tryParse(noteTitle)
        .map(path -> pathShaped(notebookName, path))
        .orElseGet(() -> titleOrAlias(notebookName, noteTitle));
  }

  static List<Note> distinctByNoteId(List<Note> notes) {
    List<Note> distinct = new ArrayList<>();
    Set<Integer> seenNoteIds = new HashSet<>();
    for (Note note : notes) {
      if (seenNoteIds.add(note.getId())) {
        distinct.add(note);
      }
    }
    return distinct;
  }

  private List<Note> titleOrAlias(String notebookName, String noteTitle) {
    List<Note> byTitle =
        noteRepository.findByNotebookNameAndNoteTitleOrderByIdAsc(notebookName, noteTitle);
    return unionByNoteId(byTitle, aliasTargets(notebookName, noteTitle));
  }

  private static List<Note> unionByNoteId(List<Note> first, List<Note> second) {
    List<Note> combined = new ArrayList<>(first);
    combined.addAll(second);
    return distinctByNoteId(combined);
  }

  private List<Note> pathShaped(String notebookName, PathShapedTarget path) {
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

  private List<Note> aliasTargets(String notebookName, String linkToken) {
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
