package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.FolderMoveRequest;
import com.odde.doughnut.controllers.dto.FolderRenameRequest;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.testability.TestabilitySettings;
import java.sql.Timestamp;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
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

  public FolderRelocationService(
      FolderRepository folderRepository,
      NoteRepository noteRepository,
      FolderSiblingNameValidation folderSiblingNameValidation,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      NoteTitlePlacementRules noteTitlePlacementRules,
      WikiLinkRewriteService wikiLinkRewriteService) {
    this.folderRepository = folderRepository;
    this.noteRepository = noteRepository;
    this.folderSiblingNameValidation = folderSiblingNameValidation;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.noteTitlePlacementRules = noteTitlePlacementRules;
    this.wikiLinkRewriteService = wikiLinkRewriteService;
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
        siblingConflictMergeTarget(notebook.getId(), destParentId, folder, request);
    if (mergeTarget.isPresent()) {
      mergeFolderInto(folder, mergeTarget.get(), now);
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
    List<Folder> subtreeFolders = collectSubtreeFolders(folder);
    Set<Integer> movedNoteIds = collectSubtreeNoteIds(subtreeFolders);

    Folder newParent = resolveNewParentFolder(request);
    if (newParent != null) {
      FolderMoveDestinationRules.requireNotMovingIntoSelfOrDescendant(folder, newParent);
      requireNewParentInNotebook(newParent, destinationNotebook);
    }

    Integer destParentId = newParent == null ? null : newParent.getId();
    Optional<Folder> mergeTarget =
        siblingConflictMergeTarget(destinationNotebook.getId(), destParentId, folder, request);
    if (mergeTarget.isPresent()) {
      mergeFolderInto(folder, mergeTarget.get(), now);
      rewriteWikiLinksForFolderMove(movedNoteIds, sourceNotebook, destinationNotebook, now, viewer);
      return mergeTarget.get();
    }

    requireNoSoftDeletedTitlesInSubtree(destinationNotebook, subtreeFolders);

    reassignFolderSubtreeToNotebook(subtreeFolders, destinationNotebook, now);
    folder.setParentFolder(newParent);
    folder.setUpdatedAt(now);
    entityPersister.flush();
    entityPersister.merge(folder);
    entityPersister.flush();
    rewriteWikiLinksForFolderMove(movedNoteIds, sourceNotebook, destinationNotebook, now, viewer);
    return folder;
  }

  /**
   * When a same-name sibling exists and merge is requested, returns that sibling; otherwise throws
   * on conflict or returns empty when the destination is free.
   */
  private Optional<Folder> siblingConflictMergeTarget(
      Integer notebookId, Integer destParentId, Folder folder, FolderMoveRequest request) {
    Optional<Folder> existingSibling =
        folderSiblingNameValidation.findConflictingSibling(
            notebookId, destParentId, folder.getName(), folder.getId());
    if (existingSibling.isEmpty()) {
      return Optional.empty();
    }
    if (request != null && request.isMerge()) {
      return existingSibling;
    }
    FolderSiblingNameValidation.throwFolderNameConflict(
        FolderSiblingNameValidation.DUPLICATE_SIBLING_NAME_HERE);
    return Optional.empty();
  }

  private void rewriteWikiLinksForFolderMove(
      Set<Integer> movedNoteIds,
      Notebook sourceNotebook,
      Notebook destinationNotebook,
      Timestamp now,
      User viewer) {
    wikiLinkRewriteService.rewriteInboundWikiLinksForFolderNotebookMove(
        movedNoteIds, destinationNotebook.getName(), now, viewer);
    wikiLinkRewriteService.rewriteOutgoingWikiLinksForFolderNotebookMove(
        movedNoteIds, sourceNotebook.getName(), now, viewer);
  }

  private Set<Integer> collectSubtreeNoteIds(List<Folder> subtreeFolders) {
    Set<Integer> noteIds = new LinkedHashSet<>();
    for (Folder subtreeFolder : subtreeFolders) {
      for (Note note : noteRepository.findNotesInFolderOrderByIdAsc(subtreeFolder.getId())) {
        noteIds.add(note.getId());
      }
    }
    return noteIds;
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

  private void requireNoSoftDeletedTitlesInSubtree(
      Notebook destinationNotebook, List<Folder> subtreeFolders) {
    for (Folder subtreeFolder : subtreeFolders) {
      for (Note note : noteRepository.findNotesInFolderOrderByIdAsc(subtreeFolder.getId())) {
        noteTitlePlacementRules.requireNoSoftDeletedTitleAt(
            destinationNotebook, subtreeFolder, note.getTitle());
      }
    }
  }

  private List<Folder> collectSubtreeFolders(Folder root) {
    List<Folder> result = new ArrayList<>();
    Deque<Folder> stack = new ArrayDeque<>();
    stack.push(root);
    while (!stack.isEmpty()) {
      Folder current = stack.pop();
      result.add(current);
      for (Folder child :
          folderRepository.findChildFoldersByParentFolderIdOrderByIdAsc(current.getId())) {
        stack.push(child);
      }
    }
    return result;
  }

  private void reassignFolderSubtreeToNotebook(
      List<Folder> subtreeFolders, Notebook destinationNotebook, Timestamp now) {
    for (Folder subtreeFolder : subtreeFolders) {
      subtreeFolder.setNotebook(destinationNotebook);
      subtreeFolder.setUpdatedAt(now);
      entityPersister.merge(subtreeFolder);
      for (Note note : noteRepository.findNotesInFolderOrderByIdAsc(subtreeFolder.getId())) {
        note.assignNotebook(destinationNotebook);
        entityPersister.merge(note);
      }
    }
  }

  private void mergeFolderInto(Folder source, Folder target, Timestamp now) {
    Notebook destinationNotebook = target.getNotebook();
    boolean crossNotebook = !source.getNotebook().getId().equals(destinationNotebook.getId());

    List<Folder> srcSubfolders =
        folderRepository.findChildFoldersByParentFolderIdOrderByIdAsc(source.getId());
    for (Folder srcChild : srcSubfolders) {
      Optional<Folder> tgtChild =
          folderRepository
              .findCandidateChildContainers(
                  destinationNotebook.getId(), target.getId(), srcChild.getName())
              .stream()
              .findFirst();
      if (tgtChild.isPresent()) {
        mergeFolderInto(srcChild, tgtChild.get(), now);
      } else {
        srcChild.setParentFolder(target);
        srcChild.setUpdatedAt(now);
        if (crossNotebook) {
          reassignFolderSubtreeToNotebook(
              collectSubtreeFolders(srcChild), destinationNotebook, now);
        }
        entityPersister.merge(srcChild);
      }
    }

    List<Note> srcNotes = noteRepository.findNotesInFolderOrderByIdAsc(source.getId());
    for (Note note : srcNotes) {
      noteTitlePlacementRules.requireNoSoftDeletedTitleAt(
          destinationNotebook, target, note.getTitle());
      note.setFolder(target);
      if (crossNotebook) {
        note.assignNotebook(destinationNotebook);
      }
      entityPersister.merge(note);
    }

    target.setUpdatedAt(now);
    entityPersister.merge(target);
    entityPersister.flush();
    entityPersister.remove(source);
  }

  public Folder renameFolder(Notebook notebook, Folder folder, FolderRenameRequest request) {
    if (!folder.getNotebook().getId().equals(notebook.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not in notebook.");
    }
    String trimmedName = request.getName().trim();
    if (trimmedName.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Folder name must not be blank.");
    }
    if (trimmedName.equals(folder.getName())) {
      return folder;
    }
    Integer parentFolderId =
        folder.getParentFolder() == null ? null : folder.getParentFolder().getId();
    folderSiblingNameValidation.requireNoConflictingSibling(
        notebook.getId(), parentFolderId, trimmedName, folder.getId());
    folder.setName(trimmedName);
    folder.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());
    entityPersister.flush();
    entityPersister.merge(folder);
    entityPersister.flush();
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
              notebook.getId(), destinationId, child.getName(), folder.getId());
      if (existingSibling.isEmpty()) {
        continue;
      }
      if (merge) {
        mergeFolderInto(child, existingSibling.get(), now);
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
