package com.odde.donut.services;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.ResolvedWikiLink;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.ResolvedWikiLinkRepository;
import jakarta.persistence.EntityManager;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Inbound resolved-wiki-link referrers for a focal note (NoteRealm references and focus-context
 * sampling).
 */
final class InboundResolvedWikiLinks {

  private final ResolvedWikiLinkRepository resolvedWikiLinkRepository;
  private final EntityManager entityManager;

  InboundResolvedWikiLinks(
      ResolvedWikiLinkRepository resolvedWikiLinkRepository, EntityManager entityManager) {
    this.resolvedWikiLinkRepository = resolvedWikiLinkRepository;
    this.entityManager = entityManager;
  }

  List<Note> referrerNotesForViewer(Note focalNote, User viewer) {
    return distinctReferrersFromTargetRows(focalNote, viewer, (row, referrer) -> true);
  }

  List<Note> referencesNotesForViewer(Note focalNote, User viewer) {
    return referrerNotesForViewer(focalNote, viewer).stream()
        .sorted(Comparator.comparing(Note::getId))
        .toList();
  }

  boolean hasRowsFromNonDeletedReferrers(Integer targetNoteId) {
    return !resolvedWikiLinkRepository
        .findRowsReferringToNonDeletedNotesForTarget(targetNoteId)
        .isEmpty();
  }

  List<Note> sampledReferencesNotesForFocusContext(
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
}
