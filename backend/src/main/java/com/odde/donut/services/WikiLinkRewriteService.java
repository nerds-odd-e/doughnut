package com.odde.donut.services;

import com.odde.donut.controllers.dto.TitleRenameReferenceHandling;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.factoryServices.EntityPersister;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rewrites wiki Portable-path links when notes move or rename. Note-ID URL links are unchanged.
 * Title rename is orchestrated here directly via {@link TitleRenameWikiLinkRewrite}; it also
 * exposes {@link WikiLinkReferenceCapture}'s pre-mutation capture. Relocation rewrite mechanics
 * live on {@link WikiLinkRelocationRewrite}, called directly by relocation callers.
 */
@Service
public class WikiLinkRewriteService {
  @PersistenceContext private EntityManager entityManager;

  private final EntityPersister entityPersister;
  private final NoteReferenceService noteReferenceService;
  private final PortablePathAuthoring portablePathAuthoring;
  private final WikiLinkResolver wikiLinkResolver;
  private final WikiLinkReferenceCapture wikiLinkReferenceCapture;

  public WikiLinkRewriteService(
      EntityPersister entityPersister,
      NoteReferenceService noteReferenceService,
      PortablePathAuthoring portablePathAuthoring,
      WikiLinkResolver wikiLinkResolver,
      WikiLinkReferenceCapture wikiLinkReferenceCapture) {
    this.entityPersister = entityPersister;
    this.noteReferenceService = noteReferenceService;
    this.portablePathAuthoring = portablePathAuthoring;
    this.wikiLinkResolver = wikiLinkResolver;
    this.wikiLinkReferenceCapture = wikiLinkReferenceCapture;
  }

  /** Persists the new title, then rewrites inbound wiki links and rebuilds referrer indexes. */
  @Transactional
  public void rewriteInboundWikiLinksForTitleRename(
      Note targetNote,
      String newTitle,
      Timestamp updatedAt,
      User viewer,
      TitleRenameReferenceHandling handling) {
    boolean keepVisible = handling == TitleRenameReferenceHandling.KEEP_VISIBLE_TEXT;
    // Capture before the rename takes effect: those references are authored against the
    // pre-rename title/aliases, so this must run before the title changes underneath them.
    Map<Integer, List<String>> inboundReferences =
        captureLiveResolvedInboundReferences(targetNote, viewer);
    TitleRenameWikiLinkRewrite.rewrite(
        entityManager,
        entityPersister,
        noteReferenceService,
        wikiLinkResolver.canonicalDonutOrigin(),
        targetNote,
        newTitle,
        updatedAt,
        inboundReferences,
        (referrer, linkText) ->
            WikiLinkRewriteSupport.rewrittenReference(
                portablePathAuthoring, referrer, targetNote, linkText, keepVisible));
  }

  /**
   * Distinct authored link text(s) per referrer note id, whose authored reference live-resolves to
   * {@code targetNote} for {@code viewer}, right now. Callers must capture this <em>before</em>
   * relocating {@code targetNote} (or the folder/notebook it lives in) — referrers are authored
   * against the pre-relocation identity, mirroring {@link TitleRenameWikiLinkRewrite}'s pre-rename
   * capture.
   */
  public Map<Integer, List<String>> captureLiveResolvedInboundReferences(
      Note targetNote, User viewer) {
    return wikiLinkReferenceCapture.liveResolvedInboundReferences(targetNote, viewer);
  }

  /**
   * {@link #captureLiveResolvedInboundReferences(Note, User)} for every live note in {@code
   * targetNoteIds}.
   */
  public Map<Integer, Map<Integer, List<String>>> captureLiveResolvedInboundReferencesByNoteId(
      Set<Integer> targetNoteIds, User viewer) {
    return wikiLinkReferenceCapture.liveResolvedInboundReferencesByNoteId(targetNoteIds, viewer);
  }

  /**
   * Each moved note's own outgoing wiki Portable-path targets that live-resolve, before the move,
   * to another note within {@code movedNoteIds} — the co-moved peers whose authored path may need
   * re-qualifying afterward. Must be captured before the move: resolution is scoped to each note's
   * pre-move notebook.
   */
  public Map<Integer, Map<String, Note>> captureLiveResolvedOutgoingWikiLinksToCoMovedNotes(
      Set<Integer> movedNoteIds, User viewer) {
    return wikiLinkReferenceCapture.liveResolvedOutgoingWikiLinksToCoMovedNotes(
        movedNoteIds, viewer);
  }
}
