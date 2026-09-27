package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Folder;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.FolderMoveRelocation;
import com.odde.donut.services.FolderSiblingNameValidation;
import java.util.List;
import org.springframework.stereotype.Service;

/** Applies eligible Folder-relocation proposals. */
@Service
class NotebookGitProposalFolderRelocation {

  private final NotebookGitProjection projection;
  private final EntityPersister entityPersister;
  private final NotebookGitStateLoader notebookGitStateLoader;
  private final FolderSiblingNameValidation folderSiblingNameValidation;
  private final FolderMoveRelocation folderMoveRelocation;
  private final NotebookGitProposalFolderMaterialization folderMaterialization;

  NotebookGitProposalFolderRelocation(
      NotebookGitProjection projection,
      EntityPersister entityPersister,
      NotebookGitStateLoader notebookGitStateLoader,
      FolderSiblingNameValidation folderSiblingNameValidation,
      FolderMoveRelocation folderMoveRelocation,
      NotebookGitProposalFolderMaterialization folderMaterialization) {
    this.projection = projection;
    this.entityPersister = entityPersister;
    this.notebookGitStateLoader = notebookGitStateLoader;
    this.folderSiblingNameValidation = folderSiblingNameValidation;
    this.folderMoveRelocation = folderMoveRelocation;
    this.folderMaterialization = folderMaterialization;
  }

  NotebookGitStateLoader.LockedNotebookState apply(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalFolderShape.FolderRelocation relocation) {
    int sourceFolderId =
        projection.folderIdAtPath(state.folders(), relocation.sourcePrefix() + "/");
    String destPrefix = relocation.destPrefix();
    String destParentPrefix = destParentPrefix(destPrefix);
    Folder destParent =
        destParentPrefix.isEmpty()
            ? null
            : folderMaterialization
                .ensureAncestry(state.notebook(), List.of(destPrefix))
                .get(destParentPrefix);
    Folder source = entityPersister.find(Folder.class, sourceFolderId);
    NotebookGitProposalFolderPlacement.requireAllowed(
        source, destParent, folderSiblingNameValidation, destPrefix);
    folderMoveRelocation.assignPlacement(source, destParent, new DisplayName(source.getName()));
    entityPersister.save(source);
    entityPersister.flush();
    return new NotebookGitStateLoader.LockedNotebookState(
        state.binding(),
        state.notebook(),
        notebookGitStateLoader.foldersOf(state.notebook()),
        state.storedNotes());
  }

  private static String destParentPrefix(String destPrefix) {
    int lastSlash = destPrefix.lastIndexOf('/');
    return lastSlash < 0 ? "" : destPrefix.substring(0, lastSlash);
  }
}
