package com.odde.donut.services;

import com.odde.donut.algorithms.NoteIdUrl;
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

/**
 * Note-local derived-index refresh plus notebook-scoped resolution refresh for resolved wiki-link
 * rows and inbound property wiki validity.
 */
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

  /**
   * Note-local refresh: rebuilds this note's property/alias/level indexes, its outgoing
   * resolved-wiki-link rows, and drops inbound property wiki rows that no longer match this note.
   */
  void refreshForNote(EntityManager entityManager, Note note, User viewer) {
    refreshDerivedIndexesForNote(note);
    refreshResolutionForNote(entityManager, note, viewer);
  }

  /**
   * Notebook resolution-scope refresh: re-resolves every live note's outgoing resolved-wiki-link
   * rows in {@code notebook} and drops inbound property wiki rows that no longer match, without
   * rebuilding property/alias/level indexes.
   *
   * <p>Portable-path resolution cardinality (unique / ambiguous / missing) depends on the whole
   * notebook's current note set, so a note's identity change (title, alias set, location) can
   * change which shorthand links in OTHER notes of that notebook resolve. Callers that change a
   * note's content-derived indexes (creation, alias frontmatter) must {@link #refreshForNote} that
   * note first so alias candidates exist before this scope re-resolves other notes.
   */
  void refreshNotebookScope(EntityManager entityManager, Notebook notebook, User viewer) {
    List<Note> liveNotes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    for (Note note : liveNotes) {
      refreshResolutionForNote(entityManager, note, viewer);
    }
  }

  private void refreshDerivedIndexesForNote(Note note) {
    notePropertyIndexService.refreshForNote(note);
    noteAliasIndexService.refreshForNote(note);
    noteLevelIndexService.refreshForNote(note);
  }

  private void refreshResolutionForNote(EntityManager entityManager, Note note, User viewer) {
    rebuildResolvedWikiLinkRows(entityManager, note, viewer);
    dropStaleInboundPropertyWikiRows(entityManager, note);
  }

  private void dropStaleInboundPropertyWikiRows(EntityManager entityManager, Note target) {
    Integer targetId = target.getId();
    String targetContent = target.getContent();
    LinkedHashSet<Integer> referrerIdsToReindex = new LinkedHashSet<>();
    for (ResolvedWikiLink row :
        resolvedWikiLinkRepository.findRowsReferringToNonDeletedNotesForTarget(targetId)) {
      if (NoteIdUrl.isAuthoredMarkdownNoteIdUrl(
          row.getAuthoredLink(), wikiLinkResolver.canonicalDonutOrigin())) {
        continue;
      }
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
