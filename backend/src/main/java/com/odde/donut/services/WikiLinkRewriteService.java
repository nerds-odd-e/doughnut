package com.odde.donut.services;

import com.odde.donut.algorithms.WikiLinkMarkdown;
import com.odde.donut.algorithms.WikiLinkMarkdownRewrite;
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
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WikiLinkRewriteService {
  @PersistenceContext private EntityManager entityManager;

  private final ResolvedWikiLinkRepository resolvedWikiLinkRepository;
  private final EntityPersister entityPersister;
  private final ResolvedWikiLinkService resolvedWikiLinkService;
  private final PortablePathAuthoring portablePathAuthoring;

  public WikiLinkRewriteService(
      ResolvedWikiLinkRepository resolvedWikiLinkRepository,
      EntityPersister entityPersister,
      ResolvedWikiLinkService resolvedWikiLinkService,
      PortablePathAuthoring portablePathAuthoring) {
    this.resolvedWikiLinkRepository = resolvedWikiLinkRepository;
    this.entityPersister = entityPersister;
    this.resolvedWikiLinkService = resolvedWikiLinkService;
    this.portablePathAuthoring = portablePathAuthoring;
  }

  /**
   * Rewrites inbound wiki links and rebuilds each changed referrer's resolved wiki-link index.
   * Persists the renamed note's new title first so updated referrer tokens resolve.
   */
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
    rewriteInboundWikiLinks(
        targetNote,
        updatedAt,
        viewer,
        (referrer, linkText) -> rewrittenTitleReference(referrer, targetNote, linkText, handling),
        Set.of());
  }

  private String rewrittenTitleReference(
      Note referrer, Note targetNote, String linkText, TitleRenameReferenceHandling handling) {
    String originalPortablePath =
        WikiLinkMarkdown.splitAuthoredToken(linkText).portablePath().format();
    String authoredPortablePath =
        portablePathAuthoring.authoredPortablePath(referrer, targetNote, originalPortablePath);
    return WikiLinkMarkdownRewrite.newInnerForAuthoredPortablePath(
        linkText, authoredPortablePath, handling == TitleRenameReferenceHandling.KEEP_VISIBLE_TEXT);
  }

  /**
   * Rewrites inbound and outgoing wiki links when a note moves to a different notebook. No-op when
   * the source and target notebooks are the same.
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

  /**
   * Rewrites inbound wiki links for a note that has moved to a different notebook. Preserves
   * visible display text while qualifying all tokens with the new notebook name.
   */
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
        (_, linkText) ->
            WikiLinkMarkdownRewrite.newInnerForKeepNotebookMove(linkText, newNotebookName),
        excludedReferrerIds);
  }

  /**
   * Rewrites inbound path-shaped wiki and Markdown links when a folder is renamed. One matching
   * folder-name segment in the prefix is updated; spelling is preserved.
   */
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

  /**
   * Rewrites inbound wiki links for every note in a folder subtree that moved to another notebook.
   * Referrers inside the moved set are skipped because their relative links still resolve.
   */
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

  /**
   * Rewrites outgoing wiki links for every note in a folder subtree that moved to another notebook.
   * Unqualified links to co-moved targets stay relative; links to notes that stayed behind qualify
   * to the source notebook.
   */
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

  /**
   * Rewrites a moved note's own unqualified outgoing wiki links so they keep pointing at its source
   * notebook after the note moves to another notebook.
   */
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
        entityManager,
        entityPersister,
        resolvedWikiLinkService,
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
        targetNote,
        updatedAt,
        viewer,
        linkRewrite,
        excludedReferrerIds);
  }
}
