package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.FolderTrailSegments;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class NoteRealmService {

  private final WikiTitleCacheService wikiTitleCacheService;
  private final NoteRepository noteRepository;

  public NoteRealmService(
      WikiTitleCacheService wikiTitleCacheService, NoteRepository noteRepository) {
    this.wikiTitleCacheService = wikiTitleCacheService;
    this.noteRepository = noteRepository;
  }

  public NoteRealm build(Note note, User viewer) {
    Note focus = hydrateNote(note);
    var wikiTitles = wikiTitleCacheService.wikiTitlesForViewer(focus, viewer);
    NoteRealm realm = new NoteRealm(focus, wikiTitles);
    List<Note> refNotes =
        hydrateNoteList(wikiTitleCacheService.referencesNotesForViewer(focus, viewer));
    realm.setReferences(refNotes.stream().map(Note::getNoteTopology).toList());
    realm.setFromBazaar(viewer == null || !viewer.owns(focus.getNotebook()));
    realm.setAncestorFolders(FolderTrailSegments.fromRootToContainingFolder(focus));
    return realm;
  }

  /** Re-load notes with associations so JSON serialization does not hit Hibernate proxies. */
  private Note hydrateNote(Note note) {
    return noteRepository
        .hydrateNonDeletedNotesWithNotebookAndFolderByIds(List.of(note.getId()))
        .stream()
        .findFirst()
        .orElse(note);
  }

  private List<Note> hydrateNoteList(List<Note> notes) {
    if (notes.isEmpty()) {
      return notes;
    }
    List<Integer> ids = notes.stream().map(Note::getId).distinct().toList();
    Map<Integer, Note> byId = new LinkedHashMap<>();
    for (Note n : noteRepository.hydrateNonDeletedNotesWithNotebookAndFolderByIds(ids)) {
      byId.putIfAbsent(n.getId(), n);
    }
    return notes.stream().map(n -> byId.getOrDefault(n.getId(), n)).toList();
  }
}
