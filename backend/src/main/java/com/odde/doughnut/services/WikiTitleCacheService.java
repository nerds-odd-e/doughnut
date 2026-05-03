package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.WikiTitle;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteWikiTitleCache;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteWikiTitleCacheRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiPredicate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WikiTitleCacheService {

  @PersistenceContext private EntityManager entityManager;

  private final WikiLinkResolver wikiLinkResolver;
  private final NoteWikiTitleCacheRepository noteWikiTitleCacheRepository;
  private final AuthorizationService authorizationService;
  private final JdbcTemplate jdbcTemplate;

  public WikiTitleCacheService(
      WikiLinkResolver wikiLinkResolver,
      NoteWikiTitleCacheRepository noteWikiTitleCacheRepository,
      AuthorizationService authorizationService,
      JdbcTemplate jdbcTemplate) {
    this.wikiLinkResolver = wikiLinkResolver;
    this.noteWikiTitleCacheRepository = noteWikiTitleCacheRepository;
    this.authorizationService = authorizationService;
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<WikiTitle> wikiTitlesForViewer(Note focusNote, User viewer) {
    List<WikiTitle> out = new ArrayList<>();
    for (NoteWikiTitleCache row :
        noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(focusNote.getId())) {
      Note resolved = authorizedOutgoingTargetNote(focusNote, row, viewer);
      if (resolved != null) {
        out.add(new WikiTitle(row.getLinkText(), resolved.getId()));
      }
    }
    return List.copyOf(out);
  }

  /**
   * Authorized outgoing wiki-link target notes for viewer (same authorization and order as {@link
   * #wikiTitlesForViewer}).
   */
  public List<Note> outgoingWikiLinkTargetNotesForViewer(Note focusNote, User viewer) {
    List<Note> notes = new ArrayList<>();
    for (WikiTitle wt : wikiTitlesForViewer(focusNote, viewer)) {
      Note n = entityManager.find(Note.class, wt.getNoteId());
      if (n != null) {
        notes.add(n);
      }
    }
    return List.copyOf(notes);
  }

  private Note authorizedOutgoingTargetNote(Note cacheOwner, NoteWikiTitleCache row, User viewer) {
    Note target = row.getTargetNote();
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
   * com.odde.doughnut.controllers.dto.NoteRealm#getReferences()} and graph RAG.
   */
  public List<Note> referencesNotesForViewer(Note focalNote, User viewer) {
    return inboundReferrerNotesForViewer(focalNote, viewer).stream()
        .sorted(Comparator.comparing(Note::getId))
        .toList();
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
  }

  private void rebuildWikiTitleCache(Note note, User viewer) {
    Integer noteId = note.getId();
    noteWikiTitleCacheRepository.deleteByNote_Id(noteId);
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
