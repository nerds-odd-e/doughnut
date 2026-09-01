package com.odde.donut.services;

import com.odde.donut.algorithms.WikiLinkMarkdown;
import com.odde.donut.algorithms.WikiLinkPropertyMatch;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.ResolvedWikiLink;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.entities.repositories.ResolvedWikiLinkRepository;
import jakarta.persistence.EntityManager;
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
public class ResolvedWikiLinkService {

  @PersistenceContext private EntityManager entityManager;

  private final AmbiguousWikiLinks ambiguousWikiLinks;
  private final ResolvedWikiLinkRepository resolvedWikiLinkRepository;
  private final AuthorizationService authorizationService;
  private final ResolvedWikiLinkRefresh resolvedWikiLinkRefresh;

  public ResolvedWikiLinkService(
      WikiLinkResolver wikiLinkResolver,
      ResolvedWikiLinkRepository resolvedWikiLinkRepository,
      AuthorizationService authorizationService,
      NotePropertyIndexService notePropertyIndexService,
      NoteAliasIndexService noteAliasIndexService,
      NoteLevelIndexService noteLevelIndexService,
      NoteRepository noteRepository) {
    this.ambiguousWikiLinks = new AmbiguousWikiLinks(wikiLinkResolver);
    this.resolvedWikiLinkRepository = resolvedWikiLinkRepository;
    this.authorizationService = authorizationService;
    this.resolvedWikiLinkRefresh =
        new ResolvedWikiLinkRefresh(
            wikiLinkResolver,
            resolvedWikiLinkRepository,
            notePropertyIndexService,
            noteAliasIndexService,
            noteLevelIndexService,
            noteRepository);
  }

  public List<WikiLink> wikiLinksForViewer(Note focusNote, User viewer) {
    List<WikiLink> out = new ArrayList<>();
    Set<String> emittedAuthored = new LinkedHashSet<>();
    for (ResolvedWikiLink row :
        resolvedWikiLinkRepository.findBySourceNote_IdOrderByIdAsc(focusNote.getId())) {
      Note resolved = authorizedOutgoingTargetNote(focusNote, row, viewer);
      if (resolved != null) {
        WikiLinkMarkdown.WikiInnerSplit parts = WikiLinkMarkdown.splitInner(row.getAuthoredLink());
        out.add(
            new WikiLink(
                row.getAuthoredLink(),
                parts.portablePath().format(),
                parts.displayText(),
                WikiLink.Resolution.RESOLVED,
                resolved.getId()));
        emittedAuthored.add(row.getAuthoredLink());
      }
    }
    out.addAll(ambiguousWikiLinks.forAuthoredTokensNotIn(focusNote, viewer, emittedAuthored));
    return List.copyOf(out);
  }

  /**
   * Authorized outgoing wiki-link target notes for viewer (same authorization as {@link
   * #wikiLinksForViewer}). Each target note appears once: multiple resolved links that share the
   * same target token (with different display text) still yield one outgoing note for graph-style
   * consumers; {@link #wikiLinksForViewer} retains one entry per distinct stored link text.
   */
  public List<Note> outgoingWikiLinkTargetNotesForViewer(Note focusNote, User viewer) {
    List<Note> notes = new ArrayList<>();
    Set<Integer> seenTargetIds = new LinkedHashSet<>();
    for (WikiLink wt : wikiLinksForViewer(focusNote, viewer)) {
      Integer id = wt.getDestinationNoteId();
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

  private Note authorizedOutgoingTargetNote(Note sourceNoteRef, ResolvedWikiLink row, User viewer) {
    Note target = row.getDestinationNote();
    if (target.getDeletedAt() != null) {
      return null;
    }
    Notebook notebook =
        target.getNotebook() != null ? target.getNotebook() : sourceNoteRef.getNotebook();
    if (!authorizationService.userMayReadNotebook(viewer, notebook)) {
      return null;
    }
    Note resolved = entityManager.find(Note.class, target.getId());
    if (resolved == null
        || !WikiLinkPropertyMatch.matchesTargetNoteContent(
            row.getAuthoredLink(), resolved.getContent())) {
      return null;
    }
    return resolved;
  }

  /**
   * Notes whose resolved wiki links point at {@code focalNote}, for {@link
   * com.odde.donut.controllers.dto.NoteRealm} inbound references. Visibility uses the referrer's
   * notebook vs the focal notebook and {@link User#canReferTo}.
   */
  public List<Note> inboundReferrerNotesForViewer(Note focalNote, User viewer) {
    return distinctReferrersFromTargetRows(focalNote, viewer, (row, referrer) -> true);
  }

  /**
   * Walks resolved wiki-link rows targeting {@code focalNote}, dedupes by referring note id,
   * applies {@code rowMatches} before visibility.
   */
  private List<Note> distinctReferrersFromTargetRows(
      Note focalNote, User viewer, BiPredicate<ResolvedWikiLink, Note> rowMatches) {
    List<ResolvedWikiLink> rows =
        resolvedWikiLinkRepository.findRowsReferringToNonDeletedNotesForTarget(focalNote.getId());
    LinkedHashMap<Integer, Note> distinctOrder = new LinkedHashMap<>();
    for (ResolvedWikiLink row : rows) {
      Integer referrerId = row.getSourceNote().getId();
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
   * Referrer notes for {@code focalNote} and {@code viewer}: all resolved wiki-link inbound links
   * ({@link #inboundReferrerNotesForViewer}), ordered by note id for {@link
   * com.odde.donut.controllers.dto.NoteRealm#getReferences()} (as topologies) and focus context
   * retrieval.
   */
  public List<Note> referencesNotesForViewer(Note focalNote, User viewer) {
    return inboundReferrerNotesForViewer(focalNote, viewer).stream()
        .sorted(Comparator.comparing(Note::getId))
        .toList();
  }

  /**
   * True when at least one non-deleted note has a resolved wiki-link row pointing at {@code
   * targetNoteId}. Used to require an explicit reference-handling choice on title rename.
   */
  public boolean hasInboundResolvedWikiLinkRowsFromNonDeletedReferrers(Integer targetNoteId) {
    return !resolvedWikiLinkRepository
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
                resolvedWikiLinkRepository.findInboundReferrersForTargetBySeedLimited(
                    focalNote.getId(),
                    focalNotebookId,
                    viewerId,
                    excludeIds,
                    Long.toString(seed),
                    cap))
        .orElseGet(
            () ->
                resolvedWikiLinkRepository.findInboundReferrersForTargetByIdAscLimited(
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
    resolvedWikiLinkRefresh.refreshForNote(entityManager, note, viewer);
  }

  @Transactional
  public void refreshNotebookScope(Notebook notebook, User viewer) {
    resolvedWikiLinkRefresh.refreshNotebookScope(entityManager, notebook, viewer);
  }
}
