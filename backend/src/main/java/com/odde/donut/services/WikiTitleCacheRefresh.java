package com.odde.donut.services;

import com.odde.donut.algorithms.WikiLinkPropertyMatch;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.NoteWikiTitleCache;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NoteWikiTitleCacheRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.LinkedHashSet;

/** Rebuilds wiki-title cache rows and drops inbound property wiki rows that no longer match. */
final class WikiTitleCacheRefresh {

  private final WikiLinkResolver wikiLinkResolver;
  private final NoteWikiTitleCacheRepository noteWikiTitleCacheRepository;
  private final NotePropertyIndexService notePropertyIndexService;
  private final NoteAliasIndexService noteAliasIndexService;

  WikiTitleCacheRefresh(
      WikiLinkResolver wikiLinkResolver,
      NoteWikiTitleCacheRepository noteWikiTitleCacheRepository,
      NotePropertyIndexService notePropertyIndexService,
      NoteAliasIndexService noteAliasIndexService) {
    this.wikiLinkResolver = wikiLinkResolver;
    this.noteWikiTitleCacheRepository = noteWikiTitleCacheRepository;
    this.notePropertyIndexService = notePropertyIndexService;
    this.noteAliasIndexService = noteAliasIndexService;
  }

  void refreshForNote(EntityManager entityManager, Note note, User viewer) {
    rebuildWikiTitleCache(entityManager, note, viewer);
    notePropertyIndexService.refreshForNote(note);
    noteAliasIndexService.refreshForNote(note);
    dropStaleInboundPropertyWikiRows(entityManager, note);
  }

  private void dropStaleInboundPropertyWikiRows(EntityManager entityManager, Note target) {
    Integer targetId = target.getId();
    String targetContent = target.getContent();
    LinkedHashSet<Integer> referrerIdsToReindex = new LinkedHashSet<>();
    for (NoteWikiTitleCache row :
        noteWikiTitleCacheRepository.findRowsReferringToNonDeletedNotesForTarget(targetId)) {
      Integer referrerId = row.getNote().getId();
      if (referrerId.equals(targetId)) {
        continue;
      }
      if (WikiLinkPropertyMatch.matchesTargetNoteContent(row.getLinkText(), targetContent)) {
        continue;
      }
      noteWikiTitleCacheRepository.delete(row);
      referrerIdsToReindex.add(referrerId);
    }
    if (referrerIdsToReindex.isEmpty()) {
      return;
    }
    entityManager.flush();
    for (Integer referrerId : referrerIdsToReindex) {
      Note referrer = entityManager.find(Note.class, referrerId);
      if (referrer != null && referrer.getDeletedAt() == null) {
        notePropertyIndexService.refreshForNote(referrer);
      }
    }
  }

  private void rebuildWikiTitleCache(EntityManager entityManager, Note note, User viewer) {
    Integer noteId = note.getId();
    entityManager.find(Note.class, noteId, LockModeType.PESSIMISTIC_WRITE);
    noteWikiTitleCacheRepository.deleteByNoteIdInBulk(noteId);
    entityManager.flush();
    Note cacheOwner = entityManager.getReference(Note.class, noteId);
    for (WikiLinkResolver.ResolvedWikiLink link :
        wikiLinkResolver.resolveWikiLinksForCache(note, viewer)) {
      NoteWikiTitleCache row = new NoteWikiTitleCache();
      row.setNote(cacheOwner);
      row.setTargetNote(entityManager.getReference(Note.class, link.targetNote().getId()));
      row.setLinkText(link.linkText());
      noteWikiTitleCacheRepository.save(row);
    }
  }
}
