package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.FolderTrailSegments;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
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
    realm.setInboundReferences(inbound);
    realm.setReferences(wikiTitleCacheService.referencesNotesForViewer(note, viewer));
    realm.setFromBazaar(viewer == null || !viewer.owns(note.getNotebook()));
    realm.setAncestorFolders(FolderTrailSegments.fromRootToContainingFolder(note));
    return realm;
  }
}
