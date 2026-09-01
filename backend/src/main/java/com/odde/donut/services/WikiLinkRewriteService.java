package com.odde.donut.services;

import com.odde.donut.controllers.dto.TitleRenameReferenceHandling;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
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
 * Public facade over {@link WikiLinkReferenceCapture} (pre-mutation capture) and {@link
 * WikiLinkRelocationRewrite} (relocation rewrite mechanics); title rename is orchestrated here
 * directly via {@link TitleRenameWikiLinkRewrite}.
 */
@Service
public class WikiLinkRewriteService {
  @PersistenceContext private EntityManager entityManager;

  private final EntityPersister entityPersister;
  private final ResolvedWikiLinkService resolvedWikiLinkService;
  private final PortablePathAuthoring portablePathAuthoring;
  private final WikiLinkResolver wikiLinkResolver;
  private final WikiLinkReferenceCapture wikiLinkReferenceCapture;
  private final WikiLinkRelocationRewrite wikiLinkRelocationRewrite;

  public WikiLinkRewriteService(
      EntityPersister entityPersister,
      ResolvedWikiLinkService resolvedWikiLinkService,
      PortablePathAuthoring portablePathAuthoring,
      WikiLinkResolver wikiLinkResolver,
      WikiLinkReferenceCapture wikiLinkReferenceCapture,
      WikiLinkRelocationRewrite wikiLinkRelocationRewrite) {
    this.entityPersister = entityPersister;
    this.resolvedWikiLinkService = resolvedWikiLinkService;
    this.portablePathAuthoring = portablePathAuthoring;
    this.wikiLinkResolver = wikiLinkResolver;
    this.wikiLinkReferenceCapture = wikiLinkReferenceCapture;
    this.wikiLinkRelocationRewrite = wikiLinkRelocationRewrite;
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
        resolvedWikiLinkService,
        wikiLinkResolver.canonicalDonutOrigin(),
        targetNote,
        newTitle,
        updatedAt,
        viewer,
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

  /**
   * Cross-notebook note move: rewrite inbound and outgoing wiki links. No-op when notebooks match.
   */
  @Transactional
  public void rewriteWikiLinksForCrossNotebookMove(
      Note movedNote,
      Notebook oldNotebook,
      Notebook targetNotebook,
      Timestamp updatedAt,
      User viewer,
      Map<Integer, List<String>> inboundReferences) {
    wikiLinkRelocationRewrite.rewriteWikiLinksForCrossNotebookMove(
        movedNote, oldNotebook, targetNotebook, updatedAt, viewer, inboundReferences);
  }

  /** Same-notebook location change: rewrite inbound exact folder/root wiki paths. */
  @Transactional
  public void rewriteInboundWikiLinksForLocationChange(
      Note targetNote,
      Timestamp updatedAt,
      User viewer,
      Map<Integer, List<String>> inboundReferences) {
    wikiLinkRelocationRewrite.rewriteInboundWikiLinksForLocationChange(
        targetNote, updatedAt, viewer, inboundReferences);
  }

  /**
   * Same-notebook folder reparent: rewrite inbound exact wiki paths for every live note in the
   * moved subtree, from referrers inside and outside the subtree.
   */
  @Transactional
  public void rewriteInboundWikiLinksForFolderReparent(
      Set<Integer> movedNoteIds,
      Timestamp updatedAt,
      User viewer,
      Map<Integer, Map<Integer, List<String>>> inboundReferencesByNoteId) {
    wikiLinkRelocationRewrite.rewriteInboundWikiLinksForFolderReparent(
        movedNoteIds, updatedAt, viewer, inboundReferencesByNoteId);
  }

  /** Folder rename: update one matching folder-name segment in inbound path-shaped wiki links. */
  @Transactional
  public void rewriteInboundWikiLinksForFolderRename(
      Set<Integer> noteIdsInSubtree,
      String oldFolderName,
      String newFolderName,
      Timestamp updatedAt,
      User viewer,
      Map<Integer, Map<Integer, List<String>>> inboundReferencesByNoteId) {
    wikiLinkRelocationRewrite.rewriteInboundWikiLinksForFolderRename(
        noteIdsInSubtree,
        oldFolderName,
        newFolderName,
        updatedAt,
        viewer,
        inboundReferencesByNoteId);
  }

  /** Cross-notebook folder move: rewrite inbound links; skip referrers inside the moved set. */
  @Transactional
  public void rewriteInboundWikiLinksForFolderNotebookMove(
      Set<Integer> movedNoteIds,
      String newNotebookName,
      Timestamp updatedAt,
      User viewer,
      Map<Integer, Map<Integer, List<String>>> inboundReferencesByNoteId) {
    wikiLinkRelocationRewrite.rewriteInboundWikiLinksForFolderNotebookMove(
        movedNoteIds, newNotebookName, updatedAt, viewer, inboundReferencesByNoteId);
  }

  /** Cross-notebook folder move: rewrite outgoing links for each note in the moved set. */
  @Transactional
  public void rewriteOutgoingWikiLinksForFolderNotebookMove(
      Set<Integer> movedNoteIds,
      String sourceNotebookName,
      Timestamp updatedAt,
      User viewer,
      Map<Integer, Map<String, Note>> coMovedTargetsByAuthoredLinkByNoteId) {
    wikiLinkRelocationRewrite.rewriteOutgoingWikiLinksForFolderNotebookMove(
        movedNoteIds, sourceNotebookName, updatedAt, viewer, coMovedTargetsByAuthoredLinkByNoteId);
  }
}
