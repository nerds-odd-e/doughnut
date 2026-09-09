package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
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
  private final NotebookGitProposalInitialCompositionPublication initialCompositionPublication;
  private final NotebookGitProposalInitialNotebookReadmePublication
      initialNotebookReadmePublication;

  NotebookGitProposalFolderAcceptance(
      NotebookGitProjection projection,
      NotebookGitProposalBindingPersistence bindingPersistence,
      EntityPersister entityPersister,
      NotebookGitStateLoader notebookGitStateLoader,
      TestabilitySettings testabilitySettings,
      FolderSiblingNameValidation folderSiblingNameValidation,
      NotebookGitProposalFolderMaterialization folderMaterialization,
      NotebookGitProposalInitialCompositionPublication initialCompositionPublication,
      NotebookGitProposalInitialNotebookReadmePublication initialNotebookReadmePublication) {
    this.projection = projection;
    this.bindingPersistence = bindingPersistence;
    this.entityPersister = entityPersister;
    this.notebookGitStateLoader = notebookGitStateLoader;
    this.testabilitySettings = testabilitySettings;
    this.folderSiblingNameValidation = folderSiblingNameValidation;
    this.folderMaterialization = folderMaterialization;
    this.initialCompositionPublication = initialCompositionPublication;
    this.initialNotebookReadmePublication = initialNotebookReadmePublication;
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

  String acceptRootFolderAndContainedNote(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      NotebookGitProposalFolderCreationShape.RootFolderAndContainedNoteCreation creation) {
    initialCompositionPublication.assertReadyEmptyNotebook(
        state,
        proposal,
        acceptedHead,
        "Initial folder Readme and contained Note require an empty notebook.");

    String folderReadme =
        NotebookGitProposalTypedPath.requireReadme(proposal, creation.folderReadmePath());
    folderMaterialization.createRootFolderWithReadme(
        state.notebook(), creation.folderReadmePath(), folderReadme);
    List<ExportFolderRow> folders = notebookGitStateLoader.foldersOf(state.notebook());
    List<Note> added =
        initialCompositionPublication.applyNotes(
            state.notebook(), folders, proposal, List.of(creation.notePath()));
    projection.requireMatchingAcceptedTree(
        state.notebook(), folders, added, proposal.repository(), proposal.mainHead());
    return bindingPersistence.accept(
        state.binding(), proposal, testabilitySettings.getCurrentUTCTimestamp());
  }

  String acceptInitialCreation(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      NotebookGitProposalFolderCreationShape.InitialNotebookAndRootFolderCreation creation) {
    List<ExportFolderRow> folders =
        createInitialNotebookAndRootFolder(
            state,
            proposal,
            acceptedHead,
            creation.notebookReadmePath(),
            creation.folderReadmePath());
    projection.requireMatchingAcceptedTree(
        state.notebook(), folders, state.liveNotes(), proposal.repository(), proposal.mainHead());
    return bindingPersistence.accept(
        state.binding(), proposal, testabilitySettings.getCurrentUTCTimestamp());
  }

  String acceptInitialCreationWithNote(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      NotebookGitProposalFolderCreationShape.InitialNotebookRootFolderAndNoteCreation creation) {
    List<ExportFolderRow> folders =
        createInitialNotebookAndRootFolder(
            state,
            proposal,
            acceptedHead,
            creation.notebookReadmePath(),
            creation.folderReadmePath());
    List<Note> added =
        initialCompositionPublication.applyNotes(
            state.notebook(), folders, proposal, List.of(creation.notePath()));
    projection.requireMatchingAcceptedTree(
        state.notebook(), folders, added, proposal.repository(), proposal.mainHead());
    return bindingPersistence.accept(
        state.binding(), proposal, testabilitySettings.getCurrentUTCTimestamp());
  }

  private List<ExportFolderRow> createInitialNotebookAndRootFolder(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      String notebookReadmePath,
      String folderReadmePath) {
    initialNotebookReadmePublication.storeOnEmptyNotebook(
        state,
        proposal,
        acceptedHead,
        notebookReadmePath,
        "Initial notebook and folder Readmes require an empty notebook.");
    String folderReadme = NotebookGitProposalTypedPath.requireReadme(proposal, folderReadmePath);
    folderMaterialization.createRootFolderWithReadme(
        state.notebook(), folderReadmePath, folderReadme);
    return notebookGitStateLoader.foldersOf(state.notebook());
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
