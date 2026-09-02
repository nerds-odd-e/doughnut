package com.odde.donut.services;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.AuthoredNoteReferenceInboundFacade;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Captures wiki-link reference state that a relocation ({@link WikiLinkRewriteService}'s note move,
 * folder move/reparent/rename/dissolve) must read <em>before</em> mutating the note, folder, or
 * notebook it targets — referrers and outgoing targets are authored against the pre-move identity,
 * so the capture happens first and the result is threaded into the rewrite call.
 */
@Service
class WikiLinkReferenceCapture {
  @PersistenceContext private EntityManager entityManager;

  private final WikiLinkResolver wikiLinkResolver;
  private final NoteReferenceService noteReferenceService;

  WikiLinkReferenceCapture(
      WikiLinkResolver wikiLinkResolver, NoteReferenceService noteReferenceService) {
    this.wikiLinkResolver = wikiLinkResolver;
    this.noteReferenceService = noteReferenceService;
  }

  /**
   * Distinct authored link text(s) per referrer note id, whose authored reference live-resolves to
   * {@code targetNote} for {@code viewer}, right now.
   */
  Map<Integer, List<String>> liveResolvedInboundReferences(Note targetNote, User viewer) {
    Map<Integer, List<String>> byReferrerId = new LinkedHashMap<>();
    for (AuthoredNoteReferenceInboundFacade.InboundReference inboundReference :
        noteReferenceService.distinctInboundReferencesForViewer(targetNote, viewer)) {
      byReferrerId.put(inboundReference.referrer().getId(), inboundReference.authoredLinkTexts());
    }
    return byReferrerId;
  }

  /** {@link #liveResolvedInboundReferences} for every live note in {@code targetNoteIds}. */
  Map<Integer, Map<Integer, List<String>>> liveResolvedInboundReferencesByNoteId(
      Set<Integer> targetNoteIds, User viewer) {
    Map<Integer, Map<Integer, List<String>>> byNoteId = new LinkedHashMap<>();
    WikiLinkRewriteSupport.forEachNonDeletedNoteInMoveSet(
        entityManager,
        targetNoteIds,
        note -> byNoteId.put(note.getId(), liveResolvedInboundReferences(note, viewer)));
    return byNoteId;
  }

  /**
   * Each moved note's own outgoing wiki Portable-path targets that live-resolve, before the move,
   * to another note within {@code movedNoteIds} — the co-moved peers whose authored path may need
   * re-qualifying afterward.
   */
  Map<Integer, Map<String, Note>> liveResolvedOutgoingWikiLinksToCoMovedNotes(
      Set<Integer> movedNoteIds, User viewer) {
    Map<Integer, Map<String, Note>> byNoteId = new LinkedHashMap<>();
    WikiLinkRewriteSupport.forEachNonDeletedNoteInMoveSet(
        entityManager,
        movedNoteIds,
        note ->
            byNoteId.put(
                note.getId(),
                WikiLinkRewriteSupport.liveResolvedOutgoingWikiLinksToNotes(
                    wikiLinkResolver, note, viewer, movedNoteIds)));
    return byNoteId;
  }
}
