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
import java.util.LinkedHashMap;
import java.util.List;
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
      Note target = row.getTargetNote();
      Notebook notebook =
          target.getNotebook() != null ? target.getNotebook() : focusNote.getNotebook();
      if (!authorizationService.userMayReadNotebook(viewer, notebook)) {
        continue;
      }
      out.add(new WikiTitle(row.getLinkText(), target.getId()));
    }
    return List.copyOf(out);
  }

  /**
   * Notes whose resolved wiki links point at {@code focalNote}, for {@link NoteRealm} inbound
   * references. Same visibility rules as legacy inbound (parent notebook vs focal notebook, {@link
   * User#canReferTo}).
   */
  public List<Note> inboundReferrerNotesForViewer(Note focalNote, User viewer) {
    List<NoteWikiTitleCache> rows =
        noteWikiTitleCacheRepository.findRowsReferringToNonDeletedNotesForTarget(focalNote.getId());
    LinkedHashMap<Integer, Note> distinctOrder = new LinkedHashMap<>();
    for (NoteWikiTitleCache row : rows) {
      Integer referrerId = row.getNote().getId();
      if (distinctOrder.containsKey(referrerId)) {
        continue;
      }
      Note referrer = entityManager.find(Note.class, referrerId);
      if (referrer == null) {
        continue;
      }
      if (inboundReferrerVisible(referrer, focalNote, viewer)) {
        distinctOrder.put(referrerId, referrer);
      }
    }
    return List.copyOf(distinctOrder.values());
  }

  private static boolean inboundReferrerVisible(Note referrer, Note focalNote, User viewer) {
    if (referrer.getParent().getNotebook() == focalNote.getNotebook()) {
      return true;
    }
    if (viewer == null) {
      return false;
    }
    return viewer.canReferTo(referrer.getParent().getNotebook());
  }

  /**
   * Replaces wiki link cache rows via JDBC—used by wiki admin migration inside a large Hibernate
   * transaction so we never enqueue {@link NoteWikiTitleCache} entities (which led to Hibernate
   * "flush after exception" failures and null identifiers).
   */
  public void replaceWikiTitleCacheRowsJdbc(Note note, User viewer) {
    Integer noteId = note.getId();
    jdbcTemplate.update("DELETE FROM note_wiki_title_cache WHERE note_id = ?", noteId);
    for (WikiLinkResolver.ResolvedWikiLink link :
        wikiLinkResolver.resolveWikiLinksForCache(note, viewer)) {
      jdbcTemplate.update(
          "INSERT INTO note_wiki_title_cache (note_id, target_note_id, link_text) VALUES (?, ?, ?) "
              + "AS new ON DUPLICATE KEY UPDATE target_note_id = new.target_note_id",
          noteId,
          link.targetNote().getId(),
          link.linkText());
    }
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
