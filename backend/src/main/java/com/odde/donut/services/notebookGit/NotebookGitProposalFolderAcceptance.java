package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Folder;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.FolderSiblingNameValidation;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.testability.TestabilitySettings;
import java.util.List;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.stereotype.Service;

/** Applies eligible Folder-creation or Folder-relocation proposals. */
@Service
class NotebookGitProposalFolderAcceptance {

  private final NotebookGitProjection projection;
  private final NotebookGitProposalBindingPersistence bindingPersistence;
  private final EntityPersister entityPersister;
  private final NotebookGitStateLoader notebookGitStateLoader;
  private final TestabilitySettings testabilitySettings;
  private final FolderSiblingNameValidation folderSiblingNameValidation;
  private final NotebookGitProposalFolderMaterialization folderMaterialization;

  NotebookGitProposalFolderAcceptance(
      NotebookGitProjection projection,
      NotebookGitProposalBindingPersistence bindingPersistence,
      EntityPersister entityPersister,
      NotebookGitStateLoader notebookGitStateLoader,
      TestabilitySettings testabilitySettings,
      FolderSiblingNameValidation folderSiblingNameValidation,
      NotebookGitProposalFolderMaterialization folderMaterialization) {
    this.projection = projection;
    this.bindingPersistence = bindingPersistence;
    this.entityPersister = entityPersister;
    this.notebookGitStateLoader = notebookGitStateLoader;
    this.testabilitySettings = testabilitySettings;
    this.folderSiblingNameValidation = folderSiblingNameValidation;
    this.folderMaterialization = folderMaterialization;
  }

  String acceptCreation(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      NotebookGitProposalFolderCreationShape.RootFolderCreation creation) {
    String readmePath = creation.readmePath();
    NotebookGitProposalMarkdownFormat.assertValidTypedMarkdown(
        proposal.repository(), proposal.mainHead());
    projection.requireMatchingAcceptedTree(
        state.notebook(), state.folders(), state.liveNotes(), proposal.repository(), acceptedHead);

    String readme = NotebookGitProposalTypedPath.requireReadme(proposal, readmePath);
    folderMaterialization.createRootFolderWithReadme(state.notebook(), readmePath, readme);

    projection.requireMatchingAcceptedTree(
        state.notebook(),
        notebookGitStateLoader.foldersOf(state.notebook()),
        state.liveNotes(),
        proposal.repository(),
        proposal.mainHead());
    return bindingPersistence.accept(
        state.binding(), proposal, testabilitySettings.getCurrentUTCTimestamp());
  }

  String accept(
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
    projection.requireMatchingAcceptedTree(
        state.notebook(),
        notebookGitStateLoader.foldersOf(state.notebook()),
        state.liveNotes(),
        proposal.repository(),
        proposal.mainHead());
    return bindingPersistence.accept(
        state.binding(), proposal, testabilitySettings.getCurrentUTCTimestamp());
  }
}
