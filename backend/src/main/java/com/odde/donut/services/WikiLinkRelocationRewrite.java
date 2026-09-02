package com.odde.donut.services;

import com.odde.donut.algorithms.WikiLinkMarkdownRewrite;
import com.odde.donut.controllers.dto.FolderTrailSegments;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.factoryServices.EntityPersister;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Wiki-link rewrite mechanics for note and folder relocation (location change, notebook move,
 * folder reparent/rename/notebook-move) for {@link WikiLinkRewriteService}, which exposes these as
 * its public API. Title rename has its own rewrite ({@link TitleRenameWikiLinkRewrite}) since it
 * discovers referrers before mutating the note rather than accepting a pre-captured set.
 */
@Service
class WikiLinkRelocationRewrite {
  @PersistenceContext private EntityManager entityManager;

  private final EntityPersister entityPersister;
  private final NoteReferenceService noteReferenceService;
  private final PortablePathAuthoring portablePathAuthoring;
  private final WikiLinkResolver wikiLinkResolver;

  WikiLinkRelocationRewrite(
      EntityPersister entityPersister,
      NoteReferenceService noteReferenceService,
      PortablePathAuthoring portablePathAuthoring,
      WikiLinkResolver wikiLinkResolver) {
    this.entityPersister = entityPersister;
    this.noteReferenceService = noteReferenceService;
    this.portablePathAuthoring = portablePathAuthoring;
    this.wikiLinkResolver = wikiLinkResolver;
  }

  /**
   * Cross-notebook note move: rewrite inbound and outgoing wiki links. No-op when notebooks match.
   */
  void rewriteWikiLinksForCrossNotebookMove(
      Note movedNote,
      Notebook oldNotebook,
      Notebook targetNotebook,
      Timestamp updatedAt,
      User viewer,
      Map<Integer, List<String>> inboundReferences) {
    Integer oldNotebookId = oldNotebook != null ? oldNotebook.getId() : null;
    if (!Objects.equals(oldNotebookId, targetNotebook.getId())) {
      rewriteInboundWikiLinksForNotebookMove(
          movedNote, targetNotebook.getName(), updatedAt, Set.of(), inboundReferences);
      String oldNotebookName = oldNotebook != null ? oldNotebook.getName() : null;
      rewriteOutgoingWikiLinksForNotebookMove(movedNote, oldNotebookName, updatedAt, viewer);
    }
  }

  /** Same-notebook location change: rewrite inbound exact folder/root wiki paths. */
  void rewriteInboundWikiLinksForLocationChange(
      Note targetNote, Timestamp updatedAt, Map<Integer, List<String>> inboundReferences) {
    List<String> folderTrail = FolderTrailSegments.namesFromRootToContainingFolder(targetNote);
    rewriteInboundWikiLinks(
        targetNote,
        updatedAt,
        (_, linkText) -> WikiLinkMarkdownRewrite.newInnerForLocationChange(linkText, folderTrail),
        Set.of(),
        inboundReferences);
  }

  /**
   * Same-notebook folder reparent: rewrite inbound exact wiki paths for every live note in the
   * moved subtree, from referrers inside and outside the subtree.
   */
  void rewriteInboundWikiLinksForFolderReparent(
      Set<Integer> movedNoteIds,
      Timestamp updatedAt,
      Map<Integer, Map<Integer, List<String>>> inboundReferencesByNoteId) {
    WikiLinkRewriteSupport.forEachNonDeletedNoteInMoveSet(
        entityManager,
        movedNoteIds,
        note ->
            rewriteInboundWikiLinksForLocationChange(
                note, updatedAt, inboundReferencesByNoteId.getOrDefault(note.getId(), Map.of())));
  }

  void rewriteInboundWikiLinksForNotebookMove(
      Note targetNote,
      String newNotebookName,
      Timestamp updatedAt,
      Set<Integer> excludedReferrerIds,
      Map<Integer, List<String>> inboundReferences) {
    rewriteInboundWikiLinks(
        targetNote,
        updatedAt,
        (referrer, linkText) ->
            rewrittenMoveReference(referrer, targetNote, linkText, newNotebookName),
        excludedReferrerIds,
        inboundReferences);
  }

  private String rewrittenMoveReference(
      Note referrer, Note targetNote, String linkText, String newNotebookName) {
    String notebookMoveRewrite =
        WikiLinkMarkdownRewrite.newInnerForKeepNotebookMove(linkText, newNotebookName);
    if (notebookMoveRewrite.equals(linkText)) {
      return linkText;
    }
    return WikiLinkRewriteSupport.rewrittenReference(
        portablePathAuthoring, referrer, targetNote, linkText, true);
  }

  /** Folder rename: update one matching folder-name segment in inbound path-shaped wiki links. */
  void rewriteInboundWikiLinksForFolderRename(
      Set<Integer> noteIdsInSubtree,
      String oldFolderName,
      String newFolderName,
      Timestamp updatedAt,
      Map<Integer, Map<Integer, List<String>>> inboundReferencesByNoteId) {
    WikiLinkRewriteSupport.forEachNonDeletedNoteInMoveSet(
        entityManager,
        noteIdsInSubtree,
        note ->
            rewriteInboundWikiLinks(
                note,
                updatedAt,
                (_, linkText) ->
                    WikiLinkMarkdownRewrite.newInnerForFolderRename(
                        linkText, oldFolderName, newFolderName),
                Set.of(),
                inboundReferencesByNoteId.getOrDefault(note.getId(), Map.of())));
  }

  /** Cross-notebook folder move: rewrite inbound links; skip referrers inside the moved set. */
  void rewriteInboundWikiLinksForFolderNotebookMove(
      Set<Integer> movedNoteIds,
      String newNotebookName,
      Timestamp updatedAt,
      Map<Integer, Map<Integer, List<String>>> inboundReferencesByNoteId) {
    WikiLinkRewriteSupport.forEachNonDeletedNoteInMoveSet(
        entityManager,
        movedNoteIds,
        note ->
            rewriteInboundWikiLinksForNotebookMove(
                note,
                newNotebookName,
                updatedAt,
                movedNoteIds,
                inboundReferencesByNoteId.getOrDefault(note.getId(), Map.of())));
  }

  /** Cross-notebook folder move: rewrite outgoing links for each note in the moved set. */
  void rewriteOutgoingWikiLinksForFolderNotebookMove(
      Set<Integer> movedNoteIds,
      String sourceNotebookName,
      Timestamp updatedAt,
      User viewer,
      Map<Integer, Map<String, Note>> coMovedTargetsByAuthoredLinkByNoteId) {
    WikiLinkRewriteSupport.forEachNonDeletedNoteInMoveSet(
        entityManager,
        movedNoteIds,
        note ->
            rewriteOutgoingWikiLinksForNotebookMove(
                note,
                sourceNotebookName,
                updatedAt,
                viewer,
                coMovedTargetsByAuthoredLinkByNoteId.getOrDefault(note.getId(), Map.of())));
  }

  /** Qualify a moved note's unqualified outgoing wiki links to the source notebook. */
  private void rewriteOutgoingWikiLinksForNotebookMove(
      Note movedNote, String sourceNotebookName, Timestamp updatedAt, User viewer) {
    rewriteOutgoingWikiLinksForNotebookMove(
        movedNote, sourceNotebookName, updatedAt, viewer, Map.of());
  }

  private void rewriteOutgoingWikiLinksForNotebookMove(
      Note movedNote,
      String sourceNotebookName,
      Timestamp updatedAt,
      User viewer,
      Map<String, Note> coMovedTargetsByAuthoredLink) {
    WikiLinkRewriteSupport.applyOutgoingNotebookMoveRewrite(
        entityPersister,
        noteReferenceService,
        portablePathAuthoring,
        wikiLinkResolver,
        wikiLinkResolver.canonicalDonutOrigin(),
        movedNote,
        sourceNotebookName,
        updatedAt,
        viewer,
        coMovedTargetsByAuthoredLink);
  }

  private void rewriteInboundWikiLinks(
      Note targetNote,
      Timestamp updatedAt,
      BiFunction<Note, String, String> linkRewrite,
      Set<Integer> excludedReferrerIds,
      Map<Integer, List<String>> inboundReferences) {
    WikiLinkRewriteSupport.applyInboundReferrerRewrite(
        entityManager,
        entityPersister,
        noteReferenceService,
        wikiLinkResolver.canonicalDonutOrigin(),
        targetNote,
        updatedAt,
        linkRewrite,
        excludedReferrerIds,
        inboundReferences);
  }
}
