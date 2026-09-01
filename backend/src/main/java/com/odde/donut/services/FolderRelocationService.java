package com.odde.donut.services;

import com.odde.donut.controllers.dto.FolderMoveRequest;
import com.odde.donut.controllers.dto.FolderRenameRequest;
import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.testability.TestabilitySettings;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FolderRelocationService {

  private final FolderRepository folderRepository;
  private final NoteRepository noteRepository;
  private final FolderSiblingNameValidation folderSiblingNameValidation;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;
  private final NoteTitlePlacementRules noteTitlePlacementRules;
  private final WikiLinkRewriteService wikiLinkRewriteService;
  private final ResolvedWikiLinkService resolvedWikiLinkService;
  private final FolderSubtree subtree;

  public FolderRelocationService(
      FolderRepository folderRepository,
      NoteRepository noteRepository,
      FolderSiblingNameValidation folderSiblingNameValidation,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      NoteTitlePlacementRules noteTitlePlacementRules,
      WikiLinkRewriteService wikiLinkRewriteService,
      ResolvedWikiLinkService resolvedWikiLinkService) {
    this.folderRepository = folderRepository;
    this.noteRepository = noteRepository;
    this.folderSiblingNameValidation = folderSiblingNameValidation;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.noteTitlePlacementRules = noteTitlePlacementRules;
    this.wikiLinkRewriteService = wikiLinkRewriteService;
    this.resolvedWikiLinkService = resolvedWikiLinkService;
    this.subtree =
        new FolderSubtree(
            folderRepository, noteRepository, entityPersister, noteTitlePlacementRules);
  }

  public Folder moveFolder(
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
    return moveFolderWithinNotebook(notebook, folder, request);
  }

  private Folder moveFolderWithinNotebook(
      Notebook notebook, Folder folder, FolderMoveRequest request) {
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

    folder.setParentFolder(newParent);
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
          movedNoteIds, sourceNotebook, destinationNotebook, now, viewer);
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
        movedNoteIds, sourceNotebook, destinationNotebook, now, viewer);
    return folder;
  }

  private void rewriteAndRefreshWikiLinksForFolderNotebookMove(
      Set<Integer> movedNoteIds,
      Notebook sourceNotebook,
      Notebook destinationNotebook,
      Timestamp now,
      User viewer) {
    wikiLinkRewriteService.rewriteInboundWikiLinksForFolderNotebookMove(
        movedNoteIds, destinationNotebook.getName(), now, viewer);
    wikiLinkRewriteService.rewriteOutgoingWikiLinksForFolderNotebookMove(
        movedNoteIds, sourceNotebook.getName(), now, viewer);
    resolvedWikiLinkService.refreshCardinalityAcrossMovedNotebooks(
        sourceNotebook, destinationNotebook, viewer);
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

  public Folder renameFolder(
      Notebook notebook, Folder folder, FolderRenameRequest request, User viewer) {
    if (!folder.getNotebook().getId().equals(notebook.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not in notebook.");
    }
    DisplayName displayName = new DisplayName(request.getName());
    String oldName = folder.getName();
    if (displayName.value().equals(oldName)) {
      return folder;
    }
    Integer parentFolderId =
        folder.getParentFolder() == null ? null : folder.getParentFolder().getId();
    folderSiblingNameValidation.requireNoConflictingSibling(
        notebook.getId(), parentFolderId, displayName, folder.getId());
    folder.setName(displayName);
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    folder.setUpdatedAt(now);
    entityPersister.flush();
    entityPersister.merge(folder);
    entityPersister.flush();
    wikiLinkRewriteService.rewriteInboundWikiLinksForFolderRename(
        subtree.collectNoteIds(subtree.collectFolders(folder)),
        oldName,
        displayName.value(),
        now,
        viewer);
    return folder;
  }

  public void dissolveFolder(Notebook notebook, Folder folder, boolean merge) {
    if (!folder.getNotebook().getId().equals(notebook.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not in notebook.");
    }

    Folder destination = folder.getParentFolder();
    Integer destinationId = destination == null ? null : destination.getId();
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();

    List<Folder> directSubfolders =
        folderRepository.findChildFoldersByParentFolderIdOrderByIdAsc(folder.getId());

    for (Folder child : directSubfolders) {
      Optional<Folder> existingSibling =
          folderSiblingNameValidation.findConflictingSibling(
              notebook.getId(), destinationId, new DisplayName(child.getName()), folder.getId());
      if (existingSibling.isEmpty()) {
        continue;
      }
      if (merge) {
        subtree.mergeInto(child, existingSibling.get(), now);
      } else {
        FolderSiblingNameValidation.throwFolderNameConflict(
            FolderSiblingNameValidation.dissolveSiblingClashAtDestination(child.getName()));
      }
    }

    List<Folder> remainingSubfolders =
        folderRepository.findChildFoldersByParentFolderIdOrderByIdAsc(folder.getId());
    for (Folder child : remainingSubfolders) {
      child.setParentFolder(destination);
      child.setUpdatedAt(now);
      entityPersister.merge(child);
    }

    List<Note> directNotes = noteRepository.findNotesInFolderOrderByIdAsc(folder.getId());
    for (Note note : directNotes) {
      noteTitlePlacementRules.requireNoSoftDeletedTitleAt(notebook, destination, note.getTitle());
      note.setFolder(destination);
      entityPersister.merge(note);
    }

    entityPersister.flush();
    entityPersister.remove(folder);
  }
}
