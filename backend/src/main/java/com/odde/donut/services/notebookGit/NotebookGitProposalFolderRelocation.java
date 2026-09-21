package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Folder;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.FolderMoveRelocation;
import com.odde.donut.services.FolderSiblingNameValidation;
import com.odde.donut.services.notebookTree.PortableTreeFolderRow;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.lib.ObjectId;
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
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      NotebookGitProposalFolderShape.FolderRelocation relocation) {
    return apply(state, proposal, acceptedHead, relocation, true);
  }

  /**
   * Reparents after the publisher already confirmed the pre-mutation state matched accepted (needed
   * when a tip parent must be materialized before destination checks).
   */
  NotebookGitStateLoader.LockedNotebookState applyAfterMatchedAcceptedTree(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      NotebookGitProposalFolderShape.FolderRelocation relocation) {
    return apply(state, proposal, acceptedHead, relocation, false);
  }

  private NotebookGitStateLoader.LockedNotebookState apply(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      NotebookGitProposalFolderShape.FolderRelocation relocation,
      boolean requireMatchingAcceptedTree) {
    List<PortableTreeFolderRow> folders = state.folders();
    int sourceFolderId =
        projection.requireRepresentedRelocationSource(
            folders, proposal.repository(), acceptedHead, relocation);
    String destPrefix = relocation.destPrefix();
    String destParentPrefix = destParentPrefix(destPrefix);
    boolean constructDestAncestry =
        !destParentPrefix.isEmpty() && !projection.hasFolderAtPath(folders, destParentPrefix + "/");
    Integer destParentFolderId =
        constructDestAncestry
            ? null
            : projection.requireRepresentedDestinationParent(
                folders,
                proposal.repository(),
                acceptedHead,
                proposal.mainHead(),
                destParentPrefix,
                destPrefix);
    projection.requireNoUnrepresentedEmptySourceDescendants(
        folders, proposal.repository(), acceptedHead, sourceFolderId);
    if (!constructDestAncestry) {
      requireAllowed(sourceFolderId, destParentFolderId, destPrefix);
    }
    NotebookGitProposalMarkdownFormat.assertValidTypedMarkdown(
        proposal.repository(), proposal.mainHead());
    if (requireMatchingAcceptedTree) {
      projection.requireMatchingAcceptedTree(
          state.notebook(), folders, state.storedNotes(), proposal.repository(), acceptedHead);
    }
    Map<String, Folder> destinationFolders =
        folderMaterialization.ensureAncestry(state.notebook(), List.of(destPrefix));
    Folder destParent =
        destParentPrefix.isEmpty() ? null : destinationFolders.get(destParentPrefix);
    if (constructDestAncestry) {
      requireAllowed(sourceFolderId, destParent == null ? null : destParent.getId(), destPrefix);
    }
    Folder source = entityPersister.find(Folder.class, sourceFolderId);
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

  private void requireAllowed(int sourceFolderId, Integer destParentFolderId, String destPrefix) {
    NotebookGitProposalFolderPlacement.requireAllowed(
        new NotebookGitProjection.RepresentedFolderRelocation(sourceFolderId, destParentFolderId),
        entityPersister,
        folderSiblingNameValidation,
        destPrefix);
  }
}
