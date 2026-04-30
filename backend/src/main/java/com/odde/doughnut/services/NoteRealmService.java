package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import org.springframework.stereotype.Service;

@Service
public class NoteRealmService {

  private final WikiLinkResolver wikiLinkResolver;

  public NoteRealmService(WikiLinkResolver wikiLinkResolver) {
    this.wikiLinkResolver = wikiLinkResolver;
  }

  public NoteRealm build(Note note, User viewer) {
    var wikiTitles = wikiLinkResolver.resolveWikiTitles(note, viewer);
    NoteRealm realm = new NoteRealm(note, wikiTitles);
    realm.setInboundReferences(
        note.getInboundReferences().stream()
            .filter(link -> inboundReferenceVisible(link, viewer))
            .toList());
    realm.setFromBazaar(viewer == null || !viewer.owns(note.getNotebook()));
    return realm;
  }

  public NoteRealm buildForNotebookRootListing(Note note, User viewer) {
    NoteRealm realm = build(note, viewer);
    realm.markNotebookRootListingShallow();
    return realm;
  }

  private boolean inboundReferenceVisible(Note inboundReference, User viewer) {
    if (inboundReference.getParent().getNotebook()
        == inboundReference.getTargetNote().getNotebook()) {
      return true;
    }
    if (viewer == null) return false;
    return viewer.canReferTo(inboundReference.getParent().getNotebook());
  }
}
