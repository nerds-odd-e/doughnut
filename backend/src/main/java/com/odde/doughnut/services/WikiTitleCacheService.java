package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.NoteFrontmatterWikiLinkTokens;
import com.odde.doughnut.controllers.dto.WikiTitle;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteWikiTitleCache;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.NoteWikiTitleCacheRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WikiTitleCacheService {

  @PersistenceContext private EntityManager entityManager;

  private final WikiLinkResolver wikiLinkResolver;
  private final NoteWikiTitleCacheRepository noteWikiTitleCacheRepository;
  private final NoteRepository noteRepository;
  private final AuthorizationService authorizationService;
  private final JdbcTemplate jdbcTemplate;

  public WikiTitleCacheService(
      WikiLinkResolver wikiLinkResolver,
      NoteWikiTitleCacheRepository noteWikiTitleCacheRepository,
      NoteRepository noteRepository,
      AuthorizationService authorizationService,
      JdbcTemplate jdbcTemplate) {
    this.wikiLinkResolver = wikiLinkResolver;
    this.noteWikiTitleCacheRepository = noteWikiTitleCacheRepository;
    this.noteRepository = noteRepository;
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
   * Authorized wiki-link targets from the focus note’s cache rows, then from each relation child
   * under the focus (repository-backed), deduped by target id in first-seen order. Used for graph
   * expansion.
   */
  public List<Note> outgoingWikiLinkedTargetsForGraph(Note focus, User viewer) {
    LinkedHashMap<Integer, Note> byTargetId = new LinkedHashMap<>();
    appendAuthorizedOutgoingTargets(focus, viewer, byTargetId);
    if (!focus.isRelation()) {
      for (Note carrier : relationCarrierChildrenOrdered(focus)) {
        appendAuthorizedOutgoingTargets(carrier, viewer, byTargetId);
      }
    }
    return List.copyOf(byTargetId.values());
  }

  /**
   * Semantic primary target for graph: prefers legacy {@link Note#getTargetNote()} when it matches
   * an outgoing wiki target or is still readable; for a non-relation focus, relation children’s
   * targets are considered in sibling order. Otherwise the first authorized outgoing target.
   */
  public Optional<Note> primaryWikiLinkedTargetForGraph(Note focus, User viewer) {
    List<Note> outgoing = outgoingWikiLinkedTargetsForGraph(focus, viewer);
    List<Note> legacyCandidates = new ArrayList<>();
    if (focus.getTargetNote() != null) {
      legacyCandidates.add(focus.getTargetNote());
    }
    if (!focus.isRelation()) {
      for (Note carrier : relationCarrierChildrenOrdered(focus)) {
        if (carrier.getTargetNote() != null) {
          legacyCandidates.add(carrier.getTargetNote());
        }
      }
    }
    for (Note legacy : legacyCandidates) {
      for (Note o : outgoing) {
        if (o.getId().equals(legacy.getId())) {
          return Optional.of(o);
        }
      }
    }
    for (Note legacy : legacyCandidates) {
      Notebook nb = legacy.getNotebook();
      if (nb != null && authorizationService.userMayReadNotebook(viewer, nb)) {
        return Optional.of(legacy);
      }
    }
    if (!outgoing.isEmpty()) {
      return Optional.of(outgoing.get(0));
    }
    return Optional.empty();
  }

  public List<Note> siblingWikiLinkReferrersToPrimaryTargetForGraph(Note focus, User viewer) {
    Optional<Note> primaryTarget = primaryWikiLinkedTargetForGraph(focus, viewer);
    if (primaryTarget.isEmpty()) {
      return List.of();
    }
    Set<Integer> excludedCarrierIds =
        relationCarrierChildrenOrdered(focus).stream().map(Note::getId).collect(Collectors.toSet());
    excludedCarrierIds.add(focus.getId());
    return referencesNotesForViewer(primaryTarget.get(), viewer).stream()
        .filter(n -> !excludedCarrierIds.contains(n.getId()))
        .toList();
  }

  private void appendAuthorizedOutgoingTargets(
      Note cacheOwner, User viewer, LinkedHashMap<Integer, Note> byTargetId) {
    for (NoteWikiTitleCache row :
        noteWikiTitleCacheRepository.findByNote_IdOrderByIdAsc(cacheOwner.getId())) {
      Note resolved = authorizedOutgoingTargetNote(cacheOwner, row, viewer);
      if (resolved != null) {
        byTargetId.putIfAbsent(resolved.getId(), resolved);
      }
    }
  }

  private List<Note> relationCarrierChildrenOrdered(Note focus) {
    return noteRepository.findAllByParentId(focus.getId()).stream()
        .filter(n -> n.getDeletedAt() == null && n.isRelation())
        .sorted(Comparator.comparing(Note::getSiblingOrder).thenComparing(Note::getId))
        .toList();
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

  /**
   * Notes whose relationship {@code source:} or non-relationship {@code parent:} wikilink resolves
   * to {@code focalNote}. Same visibility rules as {@link #inboundReferrerNotesForViewer}. For note
   * show, use {@link #referencesNotesForViewer} which merges this slice with {@link
   * #inboundReferrerNotesForViewer}.
   */
  public List<Note> subjectAndParentLinkedReferrerNotesForViewer(Note focalNote, User viewer) {
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
      Set<String> allowedNormalized =
          referrer.isRelation()
              ? NoteFrontmatterWikiLinkTokens.normalizedWikiLinkTokensFromYamlField(
                  referrer.getDetails(), "source")
              : NoteFrontmatterWikiLinkTokens.normalizedWikiLinkTokensFromYamlField(
                  referrer.getDetails(), "parent");
      if (allowedNormalized.isEmpty()) {
        continue;
      }
      String rowKey = Normalizer.normalize(row.getLinkText(), Normalizer.Form.NFKC);
      if (!allowedNormalized.contains(rowKey)) {
        continue;
      }
      if (inboundReferrerVisible(referrer, focalNote, viewer)) {
        distinctOrder.put(referrerId, referrer);
      }
    }
    return List.copyOf(distinctOrder.values());
  }

  /**
   * Merged, deduped referrer notes for {@code focalNote} and {@code viewer}: inbound wiki links
   * plus subject/parent-linked rows from the wiki-title cache, in one ordered list for {@link
   * com.odde.doughnut.controllers.dto.NoteRealm#getReferences()}.
   */
  public List<Note> referencesNotesForViewer(Note focalNote, User viewer) {
    return mergeReferenceNotes(
        inboundReferrerNotesForViewer(focalNote, viewer),
        subjectAndParentLinkedReferrerNotesForViewer(focalNote, viewer));
  }

  /**
   * Dedupes by referring note id (inbound list first, then relation-style), stable order by id
   * ascending.
   */
  static List<Note> mergeReferenceNotes(List<Note> inbound, List<Note> relationStyle) {
    LinkedHashMap<Integer, Note> byId = new LinkedHashMap<>();
    if (inbound != null) {
      for (Note n : inbound) {
        byId.putIfAbsent(n.getId(), n);
      }
    }
    if (relationStyle != null) {
      for (Note n : relationStyle) {
        byId.putIfAbsent(n.getId(), n);
      }
    }
    return byId.values().stream().sorted(Comparator.comparing(Note::getId)).toList();
  }

  private static boolean inboundReferrerVisible(Note referrer, Note focalNote, User viewer) {
    Note parent = referrer.getParent();
    if (parent == null) {
      return false;
    }
    Notebook referrerNotebook = parent.getNotebook();
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
