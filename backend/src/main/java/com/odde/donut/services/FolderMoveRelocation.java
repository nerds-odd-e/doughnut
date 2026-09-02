package com.odde.donut.services;

import com.odde.donut.controllers.dto.FolderMoveRequest;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.testability.TestabilitySettings;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Folder move (within a notebook, and cross-notebook) for {@link FolderRelocationService}. */
final class FolderMoveRelocation {

  private final FolderRepository folderRepository;
  private final FolderSiblingNameValidation folderSiblingNameValidation;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;
  private final WikiLinkRewriteService wikiLinkRewriteService;
  private final WikiLinkRelocationRewrite wikiLinkRelocationRewrite;
  private final FolderSubtree subtree;

  FolderMoveRelocation(
      FolderRepository folderRepository,
      FolderSiblingNameValidation folderSiblingNameValidation,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      WikiLinkRewriteService wikiLinkRewriteService,
      WikiLinkRelocationRewrite wikiLinkRelocationRewrite,
      FolderSubtree subtree) {
    this.folderRepository = folderRepository;
    this.folderSiblingNameValidation = folderSiblingNameValidation;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.wikiLinkRewriteService = wikiLinkRewriteService;
    this.wikiLinkRelocationRewrite = wikiLinkRelocationRewrite;
    this.subtree = subtree;
  }

  Folder moveFolder(
      Notebook notebook,
      Folder folder,
      FolderMoveRequest request,
      Notebook destinationNotebook,
      User viewer) {
    if (!folder.getNotebook().getId().equals(notebook.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not in notebook.");
    }
    if (destinationNotebook != null && !destinationNotebook.getId().equals(notebook.getId())) {
      return moveFolderToAnotherNotebook(folder, request, destinationNotebook, viewer);
    }
    return moveFolderWithinNotebook(notebook, folder, request, viewer);
  }

  private Folder moveFolderWithinNotebook(
      Notebook notebook, Folder folder, FolderMoveRequest request, User viewer) {
    Folder newParent = resolveNewParentFolder(request);
    if (newParent != null) {
      requireNewParentInNotebook(newParent, notebook);
    }
    FolderMoveDestinationRules.requireNotMovingIntoSelfOrDescendant(folder, newParent);

    Integer destParentId = newParent == null ? null : newParent.getId();
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    Optional<Folder> mergeTarget =
        folderSiblingNameValidation.mergeTargetOrRejectConflict(
            notebook.getId(), destParentId, folder, request != null && request.isMerge());
    if (mergeTarget.isPresent()) {
      subtree.mergeInto(folder, mergeTarget.get(), now);
      return mergeTarget.get();
    }

    Set<Integer> movedNoteIds = subtree.collectNoteIdsInSubtree(folder);
    Map<Integer, Map<Integer, List<String>>> inboundReferencesByNoteId =
        wikiLinkRewriteService.captureLiveResolvedInboundReferencesByNoteId(movedNoteIds, viewer);
    folder.setParentFolder(newParent);
    folder.setUpdatedAt(now);
    entityPersister.flush();
    entityPersister.merge(folder);
    entityPersister.flush();
    wikiLinkRelocationRewrite.rewriteInboundWikiLinksForFolderReparent(
        movedNoteIds, now, inboundReferencesByNoteId);
    return folder;
  }

  private Folder moveFolderToAnotherNotebook(
      Folder folder, FolderMoveRequest request, Notebook destinationNotebook, User viewer) {
    Notebook sourceNotebook = folder.getNotebook();
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    List<Folder> subtreeFolders = subtree.collectFolders(folder);
    Set<Integer> movedNoteIds = subtree.collectNoteIds(subtreeFolders);
    Map<Integer, Map<Integer, List<String>>> inboundReferencesByNoteId =
        wikiLinkRewriteService.captureLiveResolvedInboundReferencesByNoteId(movedNoteIds, viewer);
    Map<Integer, Map<String, Note>> coMovedTargetsByAuthoredLinkByNoteId =
        wikiLinkRewriteService.captureLiveResolvedOutgoingWikiLinksToCoMovedNotes(
            movedNoteIds, viewer);

    Folder newParent = resolveNewParentFolder(request);
    if (newParent != null) {
      FolderMoveDestinationRules.requireNotMovingIntoSelfOrDescendant(folder, newParent);
      requireNewParentInNotebook(newParent, destinationNotebook);
    }

    Integer destParentId = newParent == null ? null : newParent.getId();
    Optional<Folder> mergeTarget =
        folderSiblingNameValidation.mergeTargetOrRejectConflict(
            destinationNotebook.getId(),
            destParentId,
            folder,
            request != null && request.isMerge());
    if (mergeTarget.isPresent()) {
      subtree.mergeInto(folder, mergeTarget.get(), now);
      rewriteAndRefreshWikiLinksForFolderNotebookMove(
          movedNoteIds,
          sourceNotebook,
          destinationNotebook,
          now,
          viewer,
          inboundReferencesByNoteId,
          coMovedTargetsByAuthoredLinkByNoteId);
      return mergeTarget.get();
    }

    subtree.requireNoSoftDeletedTitles(destinationNotebook, subtreeFolders);

    subtree.reassignToNotebook(subtreeFolders, destinationNotebook, now);
    folder.setParentFolder(newParent);
    folder.setUpdatedAt(now);
    entityPersister.flush();
    entityPersister.merge(folder);
    entityPersister.flush();
    rewriteAndRefreshWikiLinksForFolderNotebookMove(
        movedNoteIds,
        sourceNotebook,
        destinationNotebook,
        now,
        viewer,
        inboundReferencesByNoteId,
        coMovedTargetsByAuthoredLinkByNoteId);
    return folder;
  }

  private void rewriteAndRefreshWikiLinksForFolderNotebookMove(
      Set<Integer> movedNoteIds,
      Notebook sourceNotebook,
      Notebook destinationNotebook,
      Timestamp now,
      User viewer,
      Map<Integer, Map<Integer, List<String>>> inboundReferencesByNoteId,
      Map<Integer, Map<String, Note>> coMovedTargetsByAuthoredLinkByNoteId) {
    wikiLinkRelocationRewrite.rewriteInboundWikiLinksForFolderNotebookMove(
        movedNoteIds, destinationNotebook.getName(), now, inboundReferencesByNoteId);
    wikiLinkRelocationRewrite.rewriteOutgoingWikiLinksForFolderNotebookMove(
        movedNoteIds, sourceNotebook.getName(), now, viewer, coMovedTargetsByAuthoredLinkByNoteId);
  }

  private Folder resolveNewParentFolder(FolderMoveRequest request) {
    Integer newParentFolderId = request != null ? request.getNewParentFolderId() : null;
    if (newParentFolderId == null) {
      return null;
    }
    return folderRepository
        .findById(newParentFolderId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent folder not found."));
  }

  private void requireNewParentInNotebook(Folder newParent, Notebook notebook) {
    if (!newParent.getNotebook().getId().equals(notebook.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent folder not in notebook.");
    }
  }
}
