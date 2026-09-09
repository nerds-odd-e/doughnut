package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Note;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.InspectedRegularFile;
import com.odde.donut.testability.TestabilitySettings;
import java.util.List;
import java.util.Optional;
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

  /**
   * When the notebook is empty and the inspected tree matches a wired initial composition, accepts
   * it and returns the new head; otherwise empty so the publisher can continue.
   */
  Optional<String> tryAccept(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      List<InspectedRegularFile> files) {
    if (!state.folders().isEmpty() || !state.liveNotes().isEmpty()) {
      return Optional.empty();
    }
    Optional<NotebookGitProposalInitialComposition.OneNoteInImpliedRootFolder>
        oneNoteInImpliedRootFolder =
            NotebookGitProposalInitialComposition.findOneNoteInImpliedRootFolder(files, proposal);
    if (oneNoteInImpliedRootFolder.isPresent()) {
      return Optional.of(
          acceptOneNoteInImpliedRootFolder(
              state, proposal, acceptedHead, oneNoteInImpliedRootFolder.get()));
    }
    Optional<NotebookGitProposalInitialComposition.OneNestedFolderReadme> oneNestedFolderReadme =
        NotebookGitProposalInitialComposition.findOneNestedFolderReadme(files, proposal);
    if (oneNestedFolderReadme.isPresent()) {
      return Optional.of(
          acceptOneNestedFolderReadme(state, proposal, acceptedHead, oneNestedFolderReadme.get()));
    }
    Optional<NotebookGitProposalInitialComposition.TwoSiblingRootFolderReadmes>
        twoSiblingRootFolderReadmes =
            NotebookGitProposalInitialComposition.findTwoSiblingRootFolderReadmes(files, proposal);
    if (twoSiblingRootFolderReadmes.isPresent()) {
      return Optional.of(
          acceptTwoSiblingRootFolderReadmes(
              state, proposal, acceptedHead, twoSiblingRootFolderReadmes.get()));
    }
    return Optional.empty();
  }

  private String acceptOneNoteInImpliedRootFolder(
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

  private String acceptOneNestedFolderReadme(
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

  private String acceptTwoSiblingRootFolderReadmes(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      NotebookGitProposalInitialComposition.TwoSiblingRootFolderReadmes creation) {
    String firstReadme =
        NotebookGitProposalTypedPath.requireReadme(proposal, creation.firstReadmePath());
    String secondReadme =
        NotebookGitProposalTypedPath.requireReadme(proposal, creation.secondReadmePath());
    initialNotebookReadmePublication.assertReadyEmptyNotebook(
        state, proposal, acceptedHead, "Initial sibling Folder Readmes require an empty notebook.");

    folderMaterialization.createRootFolderWithReadme(
        state.notebook(), creation.firstReadmePath(), firstReadme);
    folderMaterialization.createRootFolderWithReadme(
        state.notebook(), creation.secondReadmePath(), secondReadme);
    List<ExportFolderRow> folders = notebookGitStateLoader.foldersOf(state.notebook());
    projection.requireMatchingAcceptedTree(
        state.notebook(), folders, List.of(), proposal.repository(), proposal.mainHead());
    return bindingPersistence.accept(
        state.binding(), proposal, testabilitySettings.getCurrentUTCTimestamp());
  }
}
