package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Folder;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.FolderMoveDestinationRules;
import com.odde.donut.services.FolderSiblingNameValidation;
import org.springframework.web.server.ResponseStatusException;

/** Applies existing web folder-placement rules to an exact Git folder relocation. */
final class NotebookGitProposalFolderPlacement {

  private NotebookGitProposalFolderPlacement() {}

  static void requireAllowed(
      NotebookGitProjection.RepresentedFolderRelocation represented,
      EntityPersister entityPersister,
      FolderSiblingNameValidation folderSiblingNameValidation,
      String destPrefix) {
    Folder source = entityPersister.find(Folder.class, represented.sourceFolderId());
    Folder destParent =
        represented.destParentFolderId() == null
            ? null
            : entityPersister.find(Folder.class, represented.destParentFolderId());
    String context = "Cannot move folder to path \"" + destPrefix + "/README.md\"";
    try {
      FolderMoveDestinationRules.requireNotMovingIntoSelfOrDescendant(source, destParent);
      folderSiblingNameValidation.requireNoConflictingSibling(
          source.getNotebook().getId(),
          destParent == null ? null : destParent.getId(),
          new DisplayName(source.getName()),
          source.getId());
    } catch (ApiException exception) {
      throw exception.withContext(context);
    } catch (ResponseStatusException exception) {
      throw new ResponseStatusException(
          exception.getStatusCode(), context + ": " + exception.getReason(), exception);
    }
  }
}
