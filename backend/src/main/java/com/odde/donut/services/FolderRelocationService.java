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
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookGit.AcceptedWebChangeService;
import com.odde.donut.services.notebookGit.NotebookGitStateLoader;
import com.odde.donut.testability.TestabilitySettings;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FolderRelocationService {

  private final FolderRepository folderRepository;
  private final NoteRepository noteRepository;
  private final FolderSiblingNameValidation folderSiblingNameValidation;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;
  private final WikiLinkRewriteService wikiLinkRewriteService;
  private final WikiLinkRelocationRewrite wikiLinkRelocationRewrite;
  private final AcceptedWebChangeService acceptedWebChangeService;
  private final FolderSubtree subtree;
  private final FolderMoveRelocation folderMoveRelocation;

  public FolderRelocationService(
      FolderRepository folderRepository,
      NoteRepository noteRepository,
      FolderSiblingNameValidation folderSiblingNameValidation,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      WikiLinkRewriteService wikiLinkRewriteService,
      WikiLinkRelocationRewrite wikiLinkRelocationRewrite,
      AcceptedWebChangeService acceptedWebChangeService) {
    this.folderRepository = folderRepository;
    this.noteRepository = noteRepository;
    this.folderSiblingNameValidation = folderSiblingNameValidation;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.wikiLinkRewriteService = wikiLinkRewriteService;
    this.wikiLinkRelocationRewrite = wikiLinkRelocationRewrite;
    this.acceptedWebChangeService = acceptedWebChangeService;
    this.subtree = new FolderSubtree(folderRepository, noteRepository, entityPersister);
    this.folderMoveRelocation =
        new FolderMoveRelocation(
            folderRepository,
            folderSiblingNameValidation,
            entityPersister,
            testabilitySettings,
            wikiLinkRewriteService,
            wikiLinkRelocationRewrite,
            subtree);
  }

  @Transactional
  public Folder moveFolder(
      Notebook notebook,
      Folder folder,
      FolderMoveRequest request,
      Notebook destinationNotebook,
      User viewer) {
    return folderMoveRelocation.moveFolder(notebook, folder, request, destinationNotebook, viewer);
  }

  public Folder moveFolderWithinNotebook(
      Notebook notebook, Folder folder, FolderMoveRequest request, User viewer)
      throws UnexpectedNoAccessRightException {
    Integer folderId = folder.getId();
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    return acceptedWebChangeService.apply(
        notebook.getId(),
        locked -> {
          Notebook liveNotebook =
              locked.map(NotebookGitStateLoader.LockedNotebookState::notebook).orElse(notebook);
          Folder liveFolder =
              folderRepository
                  .findById(folderId)
                  .orElseThrow(
                      () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found."));
          return folderMoveRelocation.moveFolder(liveNotebook, liveFolder, request, null, viewer);
        },
        result -> "Move folder: " + result.getName(),
        now);
  }

  public Folder placeFolderWithinNotebook(Notebook notebook, Folder folder, Folder newParent) {
    return folderMoveRelocation.placeFolderWithinNotebook(
        notebook, folder, newParent, testabilitySettings.getCurrentUTCTimestamp());
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
    Set<Integer> noteIdsInSubtree = subtree.collectNoteIdsInSubtree(folder);
    Map<Integer, Map<Integer, List<String>>> inboundReferencesByNoteId =
        wikiLinkRewriteService.captureLiveResolvedInboundReferencesByNoteId(
            noteIdsInSubtree, viewer);
    folder.setName(displayName);
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    folder.setUpdatedAt(now);
    entityPersister.flush();
    entityPersister.merge(folder);
    entityPersister.flush();
    wikiLinkRelocationRewrite.rewriteInboundWikiLinksForFolderRename(
        noteIdsInSubtree, oldName, displayName.value(), now, inboundReferencesByNoteId);
    return folder;
  }

  public void dissolveFolder(Notebook notebook, Folder folder, boolean merge, User viewer) {
    if (!folder.getNotebook().getId().equals(notebook.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not in notebook.");
    }

    Folder destination = folder.getParentFolder();
    Integer destinationId = destination == null ? null : destination.getId();
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    Set<Integer> affectedNoteIds = subtree.collectNoteIdsInSubtree(folder);
    Map<Integer, Map<Integer, List<String>>> inboundReferencesByNoteId =
        wikiLinkRewriteService.captureLiveResolvedInboundReferencesByNoteId(
            affectedNoteIds, viewer);

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
      note.setFolder(destination);
      entityPersister.merge(note);
    }

    entityPersister.flush();
    entityPersister.remove(folder);
    entityPersister.flush();
    wikiLinkRelocationRewrite.rewriteInboundWikiLinksForFolderReparent(
        affectedNoteIds, now, inboundReferencesByNoteId);
  }
}
