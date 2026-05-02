package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.FolderTrailSegment;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NoteRealmService {

  private final WikiTitleCacheService wikiTitleCacheService;

  public NoteRealmService(WikiTitleCacheService wikiTitleCacheService) {
    this.wikiTitleCacheService = wikiTitleCacheService;
  }

  public NoteRealm build(Note note, User viewer) {
    var wikiTitles = wikiTitleCacheService.wikiTitlesForViewer(note, viewer);
    NoteRealm realm = new NoteRealm(note, wikiTitles);
    var inbound = wikiTitleCacheService.inboundReferrerNotesForViewer(note, viewer);
    var subjectOrParentLinked =
        wikiTitleCacheService.subjectAndParentLinkedReferrerNotesForViewer(note, viewer);
    realm.setInboundReferences(inbound);
    realm.setReferences(mergeReferenceNotes(inbound, subjectOrParentLinked));
    realm.setFromBazaar(viewer == null || !viewer.owns(note.getNotebook()));
    realm.setAncestorFolders(folderTrailFromRootToContainingFolder(note));
    return realm;
  }

  private static List<FolderTrailSegment> folderTrailFromRootToContainingFolder(Note note) {
    Folder folder = note.getFolder();
    if (folder == null) {
      return List.of();
    }
    List<Folder> leafToRoot = new ArrayList<>();
    for (Folder f = folder; f != null; f = f.getParentFolder()) {
      leafToRoot.add(f);
    }
    Collections.reverse(leafToRoot);
    return leafToRoot.stream().map(FolderTrailSegment::from).toList();
  }

  /**
   * Dedupes by referring note id (inbound list first, then relation-style), stable order by id
   * ascending.
   */
  static List<Note> mergeReferenceNotes(List<Note> inbound, List<Note> relationStyle) {
    LinkedHashMap<Integer, Note> byId = new LinkedHashMap<>();
    if (inbound != null) {
      for (Note n : inbound) {
        byId.putIfAbsent(n.getId(), n);
      }
    }
    if (relationStyle != null) {
      for (Note n : relationStyle) {
        byId.putIfAbsent(n.getId(), n);
      }
    }
    return byId.values().stream().sorted(Comparator.comparing(Note::getId)).toList();
  }
}
