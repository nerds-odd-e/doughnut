package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.FolderTrailSegment;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import java.util.ArrayList;
import java.util.Collections;
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
    realm.setInboundReferences(wikiTitleCacheService.inboundReferrerNotesForViewer(note, viewer));
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
}
