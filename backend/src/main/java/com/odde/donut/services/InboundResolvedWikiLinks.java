package com.odde.donut.services;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.ResolvedWikiLinkRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Inbound resolved-wiki-link focus-context sampling for a focal note. */
final class InboundResolvedWikiLinks {

  private final ResolvedWikiLinkRepository resolvedWikiLinkRepository;

  InboundResolvedWikiLinks(ResolvedWikiLinkRepository resolvedWikiLinkRepository) {
    this.resolvedWikiLinkRepository = resolvedWikiLinkRepository;
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
}
