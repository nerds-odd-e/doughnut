package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Note;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.testability.TestabilitySettings;
import java.util.List;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.stereotype.Service;

/**
 * Accepts bounded initial Readme/Note compositions recognized by {@link
 * NotebookGitProposalInitialComposition}. Materializes implied Folders without synthetic Readmes,
 * nested Folder Readmes via {@link NotebookGitProposalFolderMaterialization}, then adds Notes
 * through {@link NotebookGitProposalNoteAddition}.
 */
@Service
class NotebookGitProposalInitialCompositionPublication {

  private final NotebookGitProjection projection;
  private final NotebookGitProposalBindingPersistence bindingPersistence;
  private final NotebookGitStateLoader notebookGitStateLoader;
  private final TestabilitySettings testabilitySettings;
  private final NotebookGitProposalFolderMaterialization folderMaterialization;
  private final NotebookGitProposalNoteAddition noteAddition;
  private final NotebookGitProposalInitialNotebookReadmePublication
      initialNotebookReadmePublication;

  NotebookGitProposalInitialCompositionPublication(
      NotebookGitProjection projection,
      NotebookGitProposalBindingPersistence bindingPersistence,
      NotebookGitStateLoader notebookGitStateLoader,
      TestabilitySettings testabilitySettings,
      NotebookGitProposalFolderMaterialization folderMaterialization,
      NotebookGitProposalNoteAddition noteAddition,
      NotebookGitProposalInitialNotebookReadmePublication initialNotebookReadmePublication) {
    this.projection = projection;
    this.bindingPersistence = bindingPersistence;
    this.notebookGitStateLoader = notebookGitStateLoader;
    this.testabilitySettings = testabilitySettings;
    this.folderMaterialization = folderMaterialization;
    this.noteAddition = noteAddition;
    this.initialNotebookReadmePublication = initialNotebookReadmePublication;
  }

  String acceptOneNoteInImpliedRootFolder(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      NotebookGitProposalInitialComposition.OneNoteInImpliedRootFolder creation) {
    NotebookGitProposalTypedPath.requireOrdinaryNote(proposal, creation.notePath());
    initialNotebookReadmePublication.assertReadyEmptyNotebook(
        state,
        proposal,
        acceptedHead,
        "Initial Note in an implied root Folder requires an empty notebook.");

    String folderName =
        creation
            .impliedRootFolderPrefix()
            .substring(0, creation.impliedRootFolderPrefix().length() - 1);
    folderMaterialization.createRootFolderWithoutReadme(
        state.notebook(), creation.notePath(), folderName);
    List<ExportFolderRow> folders = notebookGitStateLoader.foldersOf(state.notebook());
    Note added =
        noteAddition.apply(
            state.notebook(),
            folders,
            proposal,
            proposal.mainHead(),
            creation.notePath(),
            testabilitySettings.getCurrentUTCTimestamp());
    projection.requireMatchingAcceptedTree(
        state.notebook(), folders, List.of(added), proposal.repository(), proposal.mainHead());
    return bindingPersistence.accept(
        state.binding(), proposal, testabilitySettings.getCurrentUTCTimestamp());
  }

  String acceptOneNestedFolderReadme(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      NotebookGitProposalInitialComposition.OneNestedFolderReadme creation) {
    String readme = NotebookGitProposalTypedPath.requireReadme(proposal, creation.readmePath());
    initialNotebookReadmePublication.assertReadyEmptyNotebook(
        state, proposal, acceptedHead, "Initial nested Folder Readme requires an empty notebook.");

    List<ExportFolderRow> folders =
        folderMaterialization.createNestedFolderWithChildReadme(
            state.notebook(),
            creation.readmePath(),
            creation.parentFolderName(),
            creation.childFolderName(),
            readme);
    projection.requireMatchingAcceptedTree(
        state.notebook(), folders, List.of(), proposal.repository(), proposal.mainHead());
    return bindingPersistence.accept(
        state.binding(), proposal, testabilitySettings.getCurrentUTCTimestamp());
  }
}
