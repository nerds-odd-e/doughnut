package com.odde.donut.services;

import com.odde.donut.algorithms.WikiLinkPropertyMatch;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.ResolvedWikiLink;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NoteRepository;
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
  private final NoteLevelIndexService noteLevelIndexService;
  private final NoteRepository noteRepository;

  ResolvedWikiLinkRefresh(
      WikiLinkResolver wikiLinkResolver,
      ResolvedWikiLinkRepository resolvedWikiLinkRepository,
      NotePropertyIndexService notePropertyIndexService,
      NoteAliasIndexService noteAliasIndexService,
      NoteLevelIndexService noteLevelIndexService,
      NoteRepository noteRepository) {
    this.wikiLinkResolver = wikiLinkResolver;
    this.resolvedWikiLinkRepository = resolvedWikiLinkRepository;
    this.notePropertyIndexService = notePropertyIndexService;
    this.noteAliasIndexService = noteAliasIndexService;
    this.noteLevelIndexService = noteLevelIndexService;
    this.noteRepository = noteRepository;
  }

  void refreshForNote(EntityManager entityManager, Note note, User viewer) {
    rebuildResolvedWikiLinkRows(entityManager, note, viewer);
    notePropertyIndexService.refreshForNote(note);
    noteAliasIndexService.refreshForNote(note);
    noteLevelIndexService.refreshForNote(note);
    dropStaleInboundPropertyWikiRows(entityManager, note);
  }

  /**
   * Re-resolves every live note's resolved wiki-link rows in {@code notebook}. Portable-path
   * resolution cardinality (unique / ambiguous / missing) depends on the whole notebook's current
   * note set, so a note's identity change (title, alias set, location) can change which shorthand
   * links in OTHER notes of that notebook resolve. This is the affected-scope re-resolution
   * operation for that broader case; {@link #refreshForNote} alone only rebuilds one note's own
   * outgoing rows.
   */
  void refreshNotebookScope(EntityManager entityManager, Notebook notebook, User viewer) {
    for (Note note : noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId())) {
      refreshForNote(entityManager, note, viewer);
    }
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
    for (WikiLinkResolver.WikiLinkResolution link :
        wikiLinkResolver.resolveWikiLinksForCache(note, viewer)) {
      ResolvedWikiLink row = new ResolvedWikiLink();
      row.setSourceNote(sourceNoteRef);
      row.setDestinationNote(
          entityManager.getReference(Note.class, link.destinationNote().getId()));
      row.setAuthoredLink(link.authoredLink());
      resolvedWikiLinkRepository.save(row);
    }
  }
}
