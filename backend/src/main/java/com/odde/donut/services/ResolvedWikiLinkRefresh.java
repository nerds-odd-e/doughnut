package com.odde.donut.services;

import com.odde.donut.algorithms.WikiLinkPropertyMatch;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.ResolvedWikiLink;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.ResolvedWikiLinkRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.LinkedHashSet;

/** Rebuilds resolved wiki-link rows and drops inbound property wiki rows that no longer match. */
final class ResolvedWikiLinkRefresh {

  private final WikiLinkResolver wikiLinkResolver;
  private final ResolvedWikiLinkRepository resolvedWikiLinkRepository;
  private final NotePropertyIndexService notePropertyIndexService;
  private final NoteAliasIndexService noteAliasIndexService;

  ResolvedWikiLinkRefresh(
      WikiLinkResolver wikiLinkResolver,
      ResolvedWikiLinkRepository resolvedWikiLinkRepository,
      NotePropertyIndexService notePropertyIndexService,
      NoteAliasIndexService noteAliasIndexService) {
    this.wikiLinkResolver = wikiLinkResolver;
    this.resolvedWikiLinkRepository = resolvedWikiLinkRepository;
    this.notePropertyIndexService = notePropertyIndexService;
    this.noteAliasIndexService = noteAliasIndexService;
  }

  void refreshForNote(EntityManager entityManager, Note note, User viewer) {
    rebuildResolvedWikiLinkRows(entityManager, note, viewer);
    notePropertyIndexService.refreshForNote(note);
    noteAliasIndexService.refreshForNote(note);
    dropStaleInboundPropertyWikiRows(entityManager, note);
  }

  private void dropStaleInboundPropertyWikiRows(EntityManager entityManager, Note target) {
    Integer targetId = target.getId();
    String targetContent = target.getContent();
    LinkedHashSet<Integer> referrerIdsToReindex = new LinkedHashSet<>();
    for (ResolvedWikiLink row :
        resolvedWikiLinkRepository.findRowsReferringToNonDeletedNotesForTarget(targetId)) {
      Integer referrerId = row.getSourceNote().getId();
      if (referrerId.equals(targetId)) {
        continue;
      }
      if (WikiLinkPropertyMatch.matchesTargetNoteContent(row.getAuthoredLink(), targetContent)) {
        continue;
      }
      resolvedWikiLinkRepository.delete(row);
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

  private void rebuildResolvedWikiLinkRows(EntityManager entityManager, Note note, User viewer) {
    Integer noteId = note.getId();
    entityManager.find(Note.class, noteId, LockModeType.PESSIMISTIC_WRITE);
    resolvedWikiLinkRepository.deleteByNoteIdInBulk(noteId);
    entityManager.flush();
    Note sourceNoteRef = entityManager.getReference(Note.class, noteId);
    for (WikiLinkResolver.ResolvedWikiLink link :
        wikiLinkResolver.resolveWikiLinksForCache(note, viewer)) {
      ResolvedWikiLink row = new ResolvedWikiLink();
      row.setSourceNote(sourceNoteRef);
      row.setDestinationNote(entityManager.getReference(Note.class, link.targetNote().getId()));
      row.setAuthoredLink(link.linkText());
      resolvedWikiLinkRepository.save(row);
    }
  }
}
