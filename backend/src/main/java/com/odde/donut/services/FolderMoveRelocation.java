package com.odde.donut.services;

import com.odde.donut.controllers.dto.FolderMoveRequest;
import com.odde.donut.entities.DisplayName;
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
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Folder placement and move (within a notebook, and cross-notebook). */
@Service
public class FolderMoveRelocation {

  private final FolderRepository folderRepository;
  private final FolderSiblingNameValidation folderSiblingNameValidation;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;
  private final WikiLinkRewriteService wikiLinkRewriteService;
  private final WikiLinkRelocationRewrite wikiLinkRelocationRewrite;
  private final FolderSubtree subtree;

  public FolderMoveRelocation(
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
    folder.requireInNotebook(notebook);
    if (destinationNotebook != null && !destinationNotebook.getId().equals(notebook.getId())) {
      return moveFolderToAnotherNotebook(folder, request, destinationNotebook, viewer);
    }
    return moveFolderWithinNotebook(notebook, folder, request, viewer);
  }

  private Folder moveFolderWithinNotebook(
      Notebook notebook, Folder folder, FolderMoveRequest request, User viewer) {
    Folder newParent = resolveNewParentFolder(request);
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    Optional<Folder> mergeTarget =
        validateDestinationAndFindMergeTarget(
            notebook, folder, newParent, request != null && request.isMerge());
    Set<Integer> movedNoteIds = subtree.collectNoteIdsInSubtree(folder);
    Map<Integer, Map<Integer, List<String>>> inboundReferencesByNoteId =
        wikiLinkRewriteService.captureLiveResolvedInboundReferencesByNoteId(movedNoteIds, viewer);
    if (mergeTarget.isPresent()) {
      subtree.mergeWithinNotebook(folder, mergeTarget.get(), now);
    } else {
      persistFolderPlacement(folder, newParent, new DisplayName(folder.getName()), now);
    }
    wikiLinkRelocationRewrite.rewriteInboundWikiLinksForFolderReparent(
        movedNoteIds, now, inboundReferencesByNoteId);
    return mergeTarget.orElse(folder);
  }

  public void assignPlacement(Folder folder, Folder newParent, DisplayName name) {
    folder.setName(name);
    folder.setParentFolder(newParent);
  }

  Folder placeFolderWithinNotebook(
      Notebook notebook, Folder folder, Folder newParent, Timestamp now) {
    folder.requireInNotebook(notebook);
    requireNewParentInNotebook(newParent, notebook);
    FolderMoveDestinationRules.requireNotMovingIntoSelfOrDescendant(folder, newParent);
    DisplayName availableName =
        folderSiblingNameValidation.firstFreeFolderName(
            notebook, newParent, new DisplayName(folder.getName()), folder.getId());
    return persistFolderPlacement(folder, newParent, availableName, now);
  }

  private Optional<Folder> validateDestinationAndFindMergeTarget(
      Notebook notebook, Folder folder, Folder newParent, boolean merge) {
    if (newParent != null) {
      requireNewParentInNotebook(newParent, notebook);
    }
    FolderMoveDestinationRules.requireNotMovingIntoSelfOrDescendant(folder, newParent);
    return folderSiblingNameValidation.mergeTargetOrRefuse(notebook, newParent, folder, merge);
  }

  private Folder persistFolderPlacement(
      Folder folder, Folder newParent, DisplayName name, Timestamp now) {
    assignPlacement(folder, newParent, name);
    folder.setUpdatedAt(now);
    entityPersister.flush();
    entityPersister.merge(folder);
    entityPersister.flush();
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

    if (!subtree.collectAttachments(subtreeFolders).isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Folders containing files cannot be moved to another notebook yet.");
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
    } else {
      subtree.reassignToNotebook(subtreeFolders, destinationNotebook, now);
      persistFolderPlacement(folder, newParent, new DisplayName(folder.getName()), now);
    }
    wikiLinkRelocationRewrite.rewriteInboundWikiLinksForFolderNotebookMove(
        movedNoteIds, destinationNotebook.getName(), now, inboundReferencesByNoteId);
    wikiLinkRelocationRewrite.rewriteOutgoingWikiLinksForFolderNotebookMove(
        movedNoteIds, sourceNotebook.getName(), now, viewer, coMovedTargetsByAuthoredLinkByNoteId);
    return mergeTarget.orElse(folder);
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
