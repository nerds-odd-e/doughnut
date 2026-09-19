package com.odde.donut.services;

import com.odde.donut.controllers.dto.FolderMoveRequest;
import com.odde.donut.controllers.dto.FolderRenameRequest;
import com.odde.donut.controllers.dto.FolderTrailSegments;
import com.odde.donut.controllers.dto.NoteTrashReferenceHandling;
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
import java.util.Set;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FolderRelocationService {

  private final FolderRepository folderRepository;
  private final FolderSiblingNameValidation folderSiblingNameValidation;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;
  private final WikiLinkRewriteService wikiLinkRewriteService;
  private final WikiLinkRelocationRewrite wikiLinkRelocationRewrite;
  private final AcceptedWebChangeService acceptedWebChangeService;
  private final FolderConstructionService folderConstructionService;
  private final AuthorizationService authorizationService;
  private final FolderSubtree subtree;
  private final FolderMoveRelocation folderMoveRelocation;
  private final NoteService noteService;

  public FolderRelocationService(
      FolderRepository folderRepository,
      NoteRepository noteRepository,
      FolderSiblingNameValidation folderSiblingNameValidation,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      WikiLinkRewriteService wikiLinkRewriteService,
      WikiLinkRelocationRewrite wikiLinkRelocationRewrite,
      AcceptedWebChangeService acceptedWebChangeService,
      FolderConstructionService folderConstructionService,
      AuthorizationService authorizationService,
      FolderMoveRelocation folderMoveRelocation,
      NoteService noteService) {
    this.folderRepository = folderRepository;
    this.folderSiblingNameValidation = folderSiblingNameValidation;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.wikiLinkRewriteService = wikiLinkRewriteService;
    this.wikiLinkRelocationRewrite = wikiLinkRelocationRewrite;
    this.acceptedWebChangeService = acceptedWebChangeService;
    this.folderConstructionService = folderConstructionService;
    this.authorizationService = authorizationService;
    this.subtree =
        new FolderSubtree(
            folderRepository, noteRepository, folderSiblingNameValidation, entityPersister);
    this.folderMoveRelocation = folderMoveRelocation;
    this.noteService = noteService;
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
    return applyLiveFolderChange(
        notebook,
        folder,
        result -> "Move folder: " + result.getName(),
        (liveNotebook, liveFolder, now) ->
            folderMoveRelocation.moveFolder(liveNotebook, liveFolder, request, null, viewer));
  }

  public Folder trashFolderWithinNotebook(Notebook notebook, Folder folder)
      throws UnexpectedNoAccessRightException {
    return applyLiveFolderChange(
        notebook,
        folder,
        result -> "Trash folder: " + result.getName(),
        (liveNotebook, liveFolder, now) -> {
          if (liveFolder.isTrashed()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Folder is already in trash.");
          }
          Folder trashParent =
              folderConstructionService.ensureTrashParentFor(
                  liveNotebook, FolderTrailSegments.ancestorsFromRootToParent(liveFolder));
          return folderMoveRelocation.placeFolderWithinNotebook(
              liveNotebook, liveFolder, trashParent, now);
        });
  }

  public void permanentlyDeleteFolderWithinNotebook(Notebook notebook, Folder folder)
      throws UnexpectedNoAccessRightException {
    applyLiveFolderChange(
        notebook,
        folder,
        result -> "Permanently delete folder: " + result.getName(),
        (liveNotebook, liveFolder, now) -> {
          if (!liveFolder.isTrashed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Folder is not in trash.");
          }
          List<Folder> subtreeFolders = subtree.collectFolders(liveFolder);
          User viewer = authorizationService.getCurrentUser();
          // Every note goes first: fk_note_folder is ON DELETE SET NULL, so a note left behind
          // would resurface at the notebook root once its folder row is gone.
          for (Note note : subtree.collectNotes(subtreeFolders)) {
            noteService.permanentlyRemove(
                note, NoteTrashReferenceHandling.LEAVE_DEAD_LINKS, viewer);
          }
          for (Folder descendantFirst : subtreeFolders.reversed()) {
            entityPersister.remove(descendantFirst);
          }
          return liveFolder;
        });
  }

  private Folder applyLiveFolderChange(
      Notebook notebook,
      Folder folder,
      Function<Folder, String> commitMessage,
      LiveFolderMutation mutation)
      throws UnexpectedNoAccessRightException {
    Integer folderId = folder.getId();
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    return acceptedWebChangeService.apply(
        notebook.getId(),
        locked -> {
          Notebook liveNotebook =
              locked
                  .state(notebook.getId())
                  .map(NotebookGitStateLoader.LockedNotebookState::notebook)
                  .orElse(notebook);
          Folder liveFolder =
              folderRepository
                  .findById(folderId)
                  .orElseThrow(
                      () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found."));
          authorizationService.assertAuthorization(liveNotebook);
          liveFolder.requireInNotebook(liveNotebook);
          return mutation.run(liveNotebook, liveFolder, now);
        },
        commitMessage,
        now);
  }

  @FunctionalInterface
  private interface LiveFolderMutation {
    Folder run(Notebook liveNotebook, Folder liveFolder, Timestamp now)
        throws UnexpectedNoAccessRightException;
  }

  public Folder renameFolder(
      Notebook notebook, Folder folder, FolderRenameRequest request, User viewer)
      throws UnexpectedNoAccessRightException {
    return applyLiveFolderChange(
        notebook,
        folder,
        result -> "Rename folder: " + result.getName(),
        (liveNotebook, liveFolder, now) ->
            renameFolderRecipe(liveNotebook, liveFolder, request, viewer, now));
  }

  private Folder renameFolderRecipe(
      Notebook notebook, Folder folder, FolderRenameRequest request, User viewer, Timestamp now) {
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
    folder.setUpdatedAt(now);
    entityPersister.flush();
    entityPersister.merge(folder);
    entityPersister.flush();
    wikiLinkRelocationRewrite.rewriteInboundWikiLinksForFolderRename(
        noteIdsInSubtree, oldName, displayName.value(), now, inboundReferencesByNoteId);
    return folder;
  }

  public void dissolveFolder(Notebook notebook, Folder folder, boolean merge, User viewer)
      throws UnexpectedNoAccessRightException {
    applyLiveFolderChange(
        notebook,
        folder,
        result -> "Dissolve folder: " + result.getName(),
        (liveNotebook, liveFolder, now) -> dissolveFolderRecipe(liveFolder, merge, viewer, now));
  }

  private Folder dissolveFolderRecipe(Folder folder, boolean merge, User viewer, Timestamp now) {
    Set<Integer> affectedNoteIds = subtree.collectNoteIdsInSubtree(folder);
    Map<Integer, Map<Integer, List<String>>> inboundReferencesByNoteId =
        wikiLinkRewriteService.captureLiveResolvedInboundReferencesByNoteId(
            affectedNoteIds, viewer);
    subtree.dissolveInto(folder, merge, now);
    wikiLinkRelocationRewrite.rewriteInboundWikiLinksForFolderReparent(
        affectedNoteIds, now, inboundReferencesByNoteId);
    return folder;
  }
}
