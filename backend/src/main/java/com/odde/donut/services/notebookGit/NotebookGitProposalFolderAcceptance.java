package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Folder;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.FolderSiblingNameValidation;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import java.util.List;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.stereotype.Service;

/** Applies eligible Folder-creation or Folder-relocation proposals. */
@Service
class NotebookGitProposalFolderAcceptance {

  private final NotebookGitProjection projection;
  private final EntityPersister entityPersister;
  private final NotebookGitStateLoader notebookGitStateLoader;
  private final FolderSiblingNameValidation folderSiblingNameValidation;
  private final NotebookGitProposalFolderMaterialization folderMaterialization;

  NotebookGitProposalFolderAcceptance(
      NotebookGitProjection projection,
      EntityPersister entityPersister,
      NotebookGitStateLoader notebookGitStateLoader,
      FolderSiblingNameValidation folderSiblingNameValidation,
      NotebookGitProposalFolderMaterialization folderMaterialization) {
    this.projection = projection;
    this.entityPersister = entityPersister;
    this.notebookGitStateLoader = notebookGitStateLoader;
    this.folderSiblingNameValidation = folderSiblingNameValidation;
    this.folderMaterialization = folderMaterialization;
  }

  NotebookGitStateLoader.LockedNotebookState applyCreation(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      NotebookGitProposalFolderCreationShape.RootFolderCreation creation) {
    String readmePath = creation.readmePath();
    NotebookGitProposalMarkdownFormat.assertValidTypedMarkdown(
        proposal.repository(), proposal.mainHead());
    projection.requireMatchingAcceptedTree(
        state.notebook(), state.folders(), state.liveNotes(), proposal.repository(), acceptedHead);

    folderMaterialization.materialize(
        state.notebook(),
        state.folders(),
        proposal.repository(),
        acceptedHead,
        List.of(readmePath),
        proposal);

    return new NotebookGitStateLoader.LockedNotebookState(
        state.binding(),
        state.notebook(),
        notebookGitStateLoader.foldersOf(state.notebook()),
        state.liveNotes());
  }

  NotebookGitStateLoader.LockedNotebookState apply(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      NotebookGitProposalFolderShape.FolderRelocation relocation) {
    List<ExportFolderRow> folders = state.folders();
    NotebookGitProjection.RepresentedFolderRelocation represented =
        projection.requireRepresentedFolderRelocation(
            folders, proposal.repository(), acceptedHead, relocation);
    projection.requireNoUnrepresentedEmptySourceDescendants(
        folders, proposal.repository(), acceptedHead, represented.sourceFolderId());
    NotebookGitProposalFolderPlacement.requireAllowed(
        represented, entityPersister, folderSiblingNameValidation, relocation.destPrefix());
    NotebookGitProposalMarkdownFormat.assertValidTypedMarkdown(
        proposal.repository(), proposal.mainHead());
    projection.requireMatchingAcceptedTree(
        state.notebook(), folders, state.liveNotes(), proposal.repository(), acceptedHead);
    Folder source = entityPersister.find(Folder.class, represented.sourceFolderId());
    source.setParentFolder(
        represented.destParentFolderId() == null
            ? null
            : entityPersister.find(Folder.class, represented.destParentFolderId()));
    entityPersister.save(source);
    entityPersister.flush();
    return new NotebookGitStateLoader.LockedNotebookState(
        state.binding(),
        state.notebook(),
        notebookGitStateLoader.foldersOf(state.notebook()),
        state.liveNotes());
  }
}
