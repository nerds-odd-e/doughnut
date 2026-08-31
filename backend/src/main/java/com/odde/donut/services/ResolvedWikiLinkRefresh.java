package com.odde.donut.services;

import com.odde.donut.algorithms.WikiLinkPropertyMatch;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.ResolvedWikiLink;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.entities.repositories.ResolvedWikiLinkRepository;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashSet;
import java.util.List;

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
    refreshDerivedIndexesForNote(note);
    dropStaleInboundPropertyWikiRows(entityManager, note);
  }

  /**
   * Re-resolves every live note's resolved wiki-link rows in {@code notebook}. Portable-path
   * resolution cardinality (unique / ambiguous / missing) depends on the whole notebook's current
   * note set, so a note's identity change (title, alias set, location) can change which shorthand
   * links in OTHER notes of that notebook resolve. This is the affected-scope re-resolution
   * operation for that broader case; {@link #refreshForNote} alone only rebuilds one note's own
   * outgoing rows.
   *
   * <p>Runs in two passes over the notebook's live notes rather than one interleaved per-note pass:
   * pass 1 rebuilds every note's own derived indexes (property/alias/level) first, then pass 2
   * resolves every note's outgoing links. A note's alias candidates live in a separate index table,
   * not on the note itself, so a single interleaved pass would resolve earlier-processed notes
   * against later notes' stale (pre-refresh) alias indexes.
   */
  void refreshNotebookScope(EntityManager entityManager, Notebook notebook, User viewer) {
    List<Note> liveNotes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    for (Note note : liveNotes) {
      refreshDerivedIndexesForNote(note);
    }
    for (Note note : liveNotes) {
      rebuildResolvedWikiLinkRows(entityManager, note, viewer);
      dropStaleInboundPropertyWikiRows(entityManager, note);
    }
  }

  private void refreshDerivedIndexesForNote(Note note) {
    notePropertyIndexService.refreshForNote(note);
    noteAliasIndexService.refreshForNote(note);
    noteLevelIndexService.refreshForNote(note);
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
