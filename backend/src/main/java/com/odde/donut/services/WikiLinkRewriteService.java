package com.odde.donut.services;

import com.odde.donut.algorithms.WikiLinkMarkdown;
import com.odde.donut.algorithms.WikiLinkMarkdownRewrite;
import com.odde.donut.controllers.dto.FolderTrailSegments;
import com.odde.donut.controllers.dto.TitleRenameReferenceHandling;
import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.ResolvedWikiLinkRepository;
import com.odde.donut.factoryServices.EntityPersister;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Rewrites wiki Portable-path links when notes move or rename. Note-ID URL links are unchanged. */
@Service
public class WikiLinkRewriteService {
  @PersistenceContext private EntityManager entityManager;

  private final ResolvedWikiLinkRepository resolvedWikiLinkRepository;
  private final EntityPersister entityPersister;
  private final ResolvedWikiLinkService resolvedWikiLinkService;
  private final PortablePathAuthoring portablePathAuthoring;
  private final WikiLinkResolver wikiLinkResolver;

  public WikiLinkRewriteService(
      ResolvedWikiLinkRepository resolvedWikiLinkRepository,
      EntityPersister entityPersister,
      ResolvedWikiLinkService resolvedWikiLinkService,
      PortablePathAuthoring portablePathAuthoring,
      WikiLinkResolver wikiLinkResolver) {
    this.resolvedWikiLinkRepository = resolvedWikiLinkRepository;
    this.entityPersister = entityPersister;
    this.resolvedWikiLinkService = resolvedWikiLinkService;
    this.portablePathAuthoring = portablePathAuthoring;
    this.wikiLinkResolver = wikiLinkResolver;
  }

  /** Persists the new title, then rewrites inbound wiki links and rebuilds referrer indexes. */
  @Transactional
  public void rewriteInboundWikiLinksForTitleRename(
      Note targetNote,
      String newTitle,
      Timestamp updatedAt,
      User viewer,
      TitleRenameReferenceHandling handling) {
    targetNote.setTitle(new DisplayName(newTitle));
    targetNote.setUpdatedAt(updatedAt);
    entityPersister.save(targetNote);
    entityManager.flush();
    boolean keepVisible = handling == TitleRenameReferenceHandling.KEEP_VISIBLE_TEXT;
    rewriteInboundWikiLinks(
        targetNote,
        updatedAt,
        viewer,
        (referrer, linkText) -> rewrittenReference(referrer, targetNote, linkText, keepVisible),
        Set.of());
  }

  private String rewrittenReference(
      Note referrer, Note targetNote, String linkText, boolean keepVisibleText) {
    String originalPortablePath = WikiLinkMarkdown.splitInner(linkText).portablePath().format();
    String authoredPortablePath =
        portablePathAuthoring.authoredPortablePath(referrer, targetNote, originalPortablePath);
    return WikiLinkMarkdownRewrite.newInnerForAuthoredPortablePath(
        linkText, authoredPortablePath, keepVisibleText);
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
      User viewer) {
    Integer oldNotebookId = oldNotebook != null ? oldNotebook.getId() : null;
    if (!Objects.equals(oldNotebookId, targetNotebook.getId())) {
      rewriteInboundWikiLinksForNotebookMove(
          movedNote, targetNotebook.getName(), updatedAt, viewer);
      String oldNotebookName = oldNotebook != null ? oldNotebook.getName() : null;
      rewriteOutgoingWikiLinksForNotebookMove(movedNote, oldNotebookName, updatedAt, viewer);
    }
  }

  /** Same-notebook location change: rewrite inbound exact folder/root wiki paths. */
  @Transactional
  public void rewriteInboundWikiLinksForLocationChange(
      Note targetNote, Timestamp updatedAt, User viewer) {
    List<String> folderTrail = FolderTrailSegments.namesFromRootToContainingFolder(targetNote);
    rewriteInboundWikiLinks(
        targetNote,
        updatedAt,
        viewer,
        (_, linkText) -> WikiLinkMarkdownRewrite.newInnerForLocationChange(linkText, folderTrail),
        Set.of());
  }

  @Transactional
  public void rewriteInboundWikiLinksForNotebookMove(
      Note targetNote, String newNotebookName, Timestamp updatedAt, User viewer) {
    rewriteInboundWikiLinksForNotebookMove(
        targetNote, newNotebookName, updatedAt, viewer, Set.of());
  }

  @Transactional
  public void rewriteInboundWikiLinksForNotebookMove(
      Note targetNote,
      String newNotebookName,
      Timestamp updatedAt,
      User viewer,
      Set<Integer> excludedReferrerIds) {
    rewriteInboundWikiLinks(
        targetNote,
        updatedAt,
        viewer,
        (referrer, linkText) ->
            rewrittenMoveReference(referrer, targetNote, linkText, newNotebookName),
        excludedReferrerIds);
  }

  private String rewrittenMoveReference(
      Note referrer, Note targetNote, String linkText, String newNotebookName) {
    String notebookMoveRewrite =
        WikiLinkMarkdownRewrite.newInnerForKeepNotebookMove(linkText, newNotebookName);
    if (notebookMoveRewrite.equals(linkText)) {
      return linkText;
    }
    return rewrittenReference(referrer, targetNote, linkText, true);
  }

  /** Folder rename: update one matching folder-name segment in inbound path-shaped wiki links. */
  @Transactional
  public void rewriteInboundWikiLinksForFolderRename(
      Set<Integer> noteIdsInSubtree,
      String oldFolderName,
      String newFolderName,
      Timestamp updatedAt,
      User viewer) {
    WikiLinkRewriteSupport.forEachNonDeletedNoteInMoveSet(
        entityManager,
        noteIdsInSubtree,
        note ->
            rewriteInboundWikiLinks(
                note,
                updatedAt,
                viewer,
                (_, linkText) ->
                    WikiLinkMarkdownRewrite.newInnerForFolderRename(
                        linkText, oldFolderName, newFolderName),
                Set.of()));
  }

  /** Cross-notebook folder move: rewrite inbound links; skip referrers inside the moved set. */
  @Transactional
  public void rewriteInboundWikiLinksForFolderNotebookMove(
      Set<Integer> movedNoteIds, String newNotebookName, Timestamp updatedAt, User viewer) {
    WikiLinkRewriteSupport.forEachNonDeletedNoteInMoveSet(
        entityManager,
        movedNoteIds,
        note ->
            rewriteInboundWikiLinksForNotebookMove(
                note, newNotebookName, updatedAt, viewer, movedNoteIds));
  }

  /** Cross-notebook folder move: rewrite outgoing links for each note in the moved set. */
  @Transactional
  public void rewriteOutgoingWikiLinksForFolderNotebookMove(
      Set<Integer> movedNoteIds, String sourceNotebookName, Timestamp updatedAt, User viewer) {
    WikiLinkRewriteSupport.forEachNonDeletedNoteInMoveSet(
        entityManager,
        movedNoteIds,
        note ->
            rewriteOutgoingWikiLinksForNotebookMove(
                note, sourceNotebookName, updatedAt, viewer, movedNoteIds));
  }

  /** Qualify a moved note's unqualified outgoing wiki links to the source notebook. */
  @Transactional
  public void rewriteOutgoingWikiLinksForNotebookMove(
      Note movedNote, String sourceNotebookName, Timestamp updatedAt, User viewer) {
    rewriteOutgoingWikiLinksForNotebookMove(
        movedNote, sourceNotebookName, updatedAt, viewer, Set.of());
  }

  @Transactional
  public void rewriteOutgoingWikiLinksForNotebookMove(
      Note movedNote,
      String sourceNotebookName,
      Timestamp updatedAt,
      User viewer,
      Set<Integer> coMovedTargetNoteIds) {
    WikiLinkRewriteSupport.applyOutgoingNotebookMoveRewrite(
        resolvedWikiLinkRepository,
        entityPersister,
        resolvedWikiLinkService,
        portablePathAuthoring,
        wikiLinkResolver,
        movedNote,
        sourceNotebookName,
        updatedAt,
        viewer,
        coMovedTargetNoteIds);
  }

  private void rewriteInboundWikiLinks(
      Note targetNote,
      Timestamp updatedAt,
      User viewer,
      BiFunction<Note, String, String> linkRewrite,
      Set<Integer> excludedReferrerIds) {
    WikiLinkRewriteSupport.applyInboundReferrerRewrite(
        entityManager,
        resolvedWikiLinkRepository,
        entityPersister,
        resolvedWikiLinkService,
        wikiLinkResolver.canonicalDonutOrigin(),
        targetNote,
        updatedAt,
        viewer,
        linkRewrite,
        excludedReferrerIds);
  }
}
