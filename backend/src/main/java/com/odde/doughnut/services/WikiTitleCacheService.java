package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.WikiLinkMarkdown;
import com.odde.doughnut.controllers.dto.WikiTitle;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteWikiTitleCache;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteWikiTitleCacheRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WikiTitleCacheService {

  @PersistenceContext private EntityManager entityManager;

  private final WikiLinkResolver wikiLinkResolver;
  private final NoteWikiTitleCacheRepository noteWikiTitleCacheRepository;
  private final AuthorizationService authorizationService;
  private final NotePropertyIndexService notePropertyIndexService;
  private final NoteAliasIndexService noteAliasIndexService;

  public WikiTitleCacheService(
      WikiLinkResolver wikiLinkResolver,
      NoteWikiTitleCacheRepository noteWikiTitleCacheRepository,
      AuthorizationService authorizationService,
      NotePropertyIndexService notePropertyIndexService,
      NoteAliasIndexService noteAliasIndexService) {
    this.wikiLinkResolver = wikiLinkResolver;
    this.noteWikiTitleCacheRepository = noteWikiTitleCacheRepository;
    this.authorizationService = authorizationService;
    this.notePropertyIndexService = notePropertyIndexService;
    this.noteAliasIndexService = noteAliasIndexService;
  }

  public List<WikiTitle> wikiTitlesForViewer(Note focusNote, User viewer) {
    List<WikiTitle> out = new ArrayList<>();
    for (NoteWikiTitleCache row :
        noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(focusNote.getId())) {
      Note resolved = authorizedOutgoingTargetNote(focusNote, row, viewer);
      if (resolved != null) {
        WikiLinkMarkdown.WikiInnerSplit parts = WikiLinkMarkdown.splitInner(row.getLinkText());
        out.add(
            new WikiTitle(row.getLinkText(), parts.target(), parts.display(), resolved.getId()));
      }
    }
    return List.copyOf(out);
  }

  /**
   * Authorized outgoing wiki-link target notes for viewer (same authorization as {@link
   * #wikiTitlesForViewer}). Each target note appears once: multiple resolved links that share the
   * same target token (with different display text) still yield one outgoing note for graph-style
   * consumers; {@link #wikiTitlesForViewer} retains one entry per distinct stored link text.
   */
  public List<Note> outgoingWikiLinkTargetNotesForViewer(Note focusNote, User viewer) {
    List<Note> notes = new ArrayList<>();
    Set<Integer> seenTargetIds = new LinkedHashSet<>();
    for (WikiTitle wt : wikiTitlesForViewer(focusNote, viewer)) {
      Integer id = wt.getNoteId();
      if (id == null || !seenTargetIds.add(id)) {
        continue;
      }
      Note n = entityManager.find(Note.class, id);
      if (n != null) {
        notes.add(n);
      }
    }
    return List.copyOf(notes);
  }

  private Note authorizedOutgoingTargetNote(Note cacheOwner, NoteWikiTitleCache row, User viewer) {
    Note target = row.getTargetNote();
    if (target.getDeletedAt() != null) {
      return null;
    }
    Notebook notebook =
        target.getNotebook() != null ? target.getNotebook() : cacheOwner.getNotebook();
    if (!authorizationService.userMayReadNotebook(viewer, notebook)) {
      return null;
    }
    return entityManager.find(Note.class, target.getId());
  }

  /**
   * Notes whose resolved wiki links point at {@code focalNote}, for {@link
   * com.odde.doughnut.controllers.dto.NoteRealm} inbound references. Visibility uses the referrer's
   * notebook vs the focal notebook and {@link User#canReferTo}.
   */
  public List<Note> inboundReferrerNotesForViewer(Note focalNote, User viewer) {
    return distinctReferrersFromTargetRows(focalNote, viewer, (row, referrer) -> true);
  }

  /**
   * Walks wiki cache rows targeting {@code focalNote}, dedupes by referring note id, applies {@code
   * rowMatches} before visibility.
   */
  private List<Note> distinctReferrersFromTargetRows(
      Note focalNote, User viewer, BiPredicate<NoteWikiTitleCache, Note> rowMatches) {
    List<NoteWikiTitleCache> rows =
        noteWikiTitleCacheRepository.findRowsReferringToNonDeletedNotesForTarget(focalNote.getId());
    LinkedHashMap<Integer, Note> distinctOrder = new LinkedHashMap<>();
    for (NoteWikiTitleCache row : rows) {
      Integer referrerId = row.getNote().getId();
      if (distinctOrder.containsKey(referrerId)) {
        continue;
      }
      Note referrer = entityManager.find(Note.class, referrerId);
      if (referrer == null || !rowMatches.test(row, referrer)) {
        continue;
      }
      if (inboundReferrerVisible(referrer, focalNote, viewer)) {
        distinctOrder.put(referrerId, referrer);
      }
    }
    return List.copyOf(distinctOrder.values());
  }

  /**
   * Referrer notes for {@code focalNote} and {@code viewer}: all wiki-title cache inbound links
   * ({@link #inboundReferrerNotesForViewer}), ordered by note id for {@link
   * com.odde.doughnut.controllers.dto.NoteRealm#getReferences()} (as topologies) and focus context
   * retrieval.
   */
  public List<Note> referencesNotesForViewer(Note focalNote, User viewer) {
    return inboundReferrerNotesForViewer(focalNote, viewer).stream()
        .sorted(Comparator.comparing(Note::getId))
        .toList();
  }

  /**
   * True when at least one non-deleted note has a wiki-title cache row pointing at {@code
   * targetNoteId}. Used to require an explicit reference-handling choice on title rename.
   */
  public boolean hasInboundWikiTitleCacheRowsFromNonDeletedReferrers(Integer targetNoteId) {
    return !noteWikiTitleCacheRepository
        .findRowsReferringToNonDeletedNotesForTarget(targetNoteId)
        .isEmpty();
  }

  /**
   * Inbound referrers for focus-context only: same visibility as {@link #referencesNotesForViewer},
   * distinct by referrer id, excluding {@code excludeNoteIds}, capped in the database.
   */
  public List<Note> sampledReferencesNotesForFocusContext(
      Note focalNote,
      User viewer,
      Set<Integer> excludeNoteIds,
      int cap,
      Optional<Long> sampleSeed) {
    if (cap <= 0 || focalNote.getId() == null) {
      return List.of();
    }
    Integer focalNotebookId =
        focalNote.getNotebook() != null ? focalNote.getNotebook().getId() : null;
    Integer viewerId = viewer != null ? viewer.getId() : null;
    List<Integer> excludeIds = excludeIdsForNativeIn(excludeNoteIds);
    return sampleSeed
        .map(
            seed ->
                noteWikiTitleCacheRepository.findInboundReferrersForTargetBySeedLimited(
                    focalNote.getId(),
                    focalNotebookId,
                    viewerId,
                    excludeIds,
                    Long.toString(seed),
                    cap))
        .orElseGet(
            () ->
                noteWikiTitleCacheRepository.findInboundReferrersForTargetByIdAscLimited(
                    focalNote.getId(), focalNotebookId, viewerId, excludeIds, cap));
  }

  private static List<Integer> excludeIdsForNativeIn(Set<Integer> excludeNoteIds) {
    LinkedHashSet<Integer> ids = new LinkedHashSet<>();
    for (Integer id : excludeNoteIds) {
      if (id != null) {
        ids.add(id);
      }
    }
    if (ids.isEmpty()) {
      return List.of(-1);
    }
    return List.copyOf(ids);
  }

  private static boolean inboundReferrerVisible(Note referrer, Note focalNote, User viewer) {
    Notebook referrerNotebook = referrer.getNotebook();
    Notebook focalNotebook = focalNote.getNotebook();
    if (referrerNotebook != null
        && focalNotebook != null
        && referrerNotebook.getId().equals(focalNotebook.getId())) {
      return true;
    }
    if (viewer == null || referrerNotebook == null) {
      return false;
    }
    return viewer.canReferTo(referrerNotebook);
  }

  @Transactional
  public void refreshForNote(Note note, User viewer) {
    rebuildWikiTitleCache(note, viewer);
    notePropertyIndexService.refreshForNote(note);
    noteAliasIndexService.refreshForNote(note);
  }

  private void rebuildWikiTitleCache(Note note, User viewer) {
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
