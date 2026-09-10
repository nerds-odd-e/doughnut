package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.InspectedRegularFile;
import com.odde.donut.testability.TestabilitySettings;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Accepts bounded initial Readme/Note compositions recognized by {@link
 * NotebookGitProposalInitialComposition}. Materializes implied Folders without synthetic Readmes,
 * nested Folder Readmes via {@link NotebookGitProposalFolderMaterialization}, then adds Notes
 * through {@link #applyNotes} and {@link NotebookGitProposalNoteAddition}.
 */
@Service
class NotebookGitProposalInitialCompositionPublication {

  private final NotebookGitProjection projection;
  private final NotebookGitProposalBindingPersistence bindingPersistence;
  private final NotebookGitStateLoader notebookGitStateLoader;
  private final TestabilitySettings testabilitySettings;
  private final NotebookGitProposalFolderMaterialization folderMaterialization;
  private final NotebookGitProposalNoteAddition noteAddition;

  NotebookGitProposalInitialCompositionPublication(
      NotebookGitProjection projection,
      NotebookGitProposalBindingPersistence bindingPersistence,
      NotebookGitStateLoader notebookGitStateLoader,
      TestabilitySettings testabilitySettings,
      NotebookGitProposalFolderMaterialization folderMaterialization,
      NotebookGitProposalNoteAddition noteAddition) {
    this.projection = projection;
    this.bindingPersistence = bindingPersistence;
    this.notebookGitStateLoader = notebookGitStateLoader;
    this.testabilitySettings = testabilitySettings;
    this.folderMaterialization = folderMaterialization;
    this.noteAddition = noteAddition;
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
    Optional<NotebookGitProposalInitialComposition.NotesInImpliedRootFolder>
        notesInImpliedRootFolder =
            NotebookGitProposalInitialComposition.findNotesInImpliedRootFolder(files, proposal);
    if (notesInImpliedRootFolder.isPresent()) {
      return Optional.of(
          acceptNotesInImpliedRootFolder(
              state, proposal, acceptedHead, notesInImpliedRootFolder.get()));
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

  /**
   * Applies an already-classified list of ordinary Note paths through {@link
   * NotebookGitProposalNoteAddition}, accumulating created Notes for projection proof. Does not
   * accept the binding.
   */
  List<Note> applyNotes(
      Notebook notebook,
      List<ExportFolderRow> folders,
      NotebookGitProposalImporter.ImportedProposal proposal,
      List<String> notePaths) {
    List<Note> added = new ArrayList<>(notePaths.size());
    for (String notePath : notePaths) {
      NotebookGitProposalTypedPath.requireOrdinaryNote(proposal, notePath);
      added.add(applyConcept(notebook, folders, proposal, notePath));
    }
    return added;
  }

  /** Applies one authored concept path through {@link NotebookGitProposalNoteAddition}. */
  Note applyConcept(
      Notebook notebook,
      List<ExportFolderRow> folders,
      NotebookGitProposalImporter.ImportedProposal proposal,
      String conceptPath) {
    return noteAddition.apply(
        notebook,
        folders,
        proposal,
        proposal.mainHead(),
        conceptPath,
        testabilitySettings.getCurrentUTCTimestamp());
  }

  void assertReadyEmptyNotebook(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      String emptyNotebookMessage) {
    NotebookGitProposalMarkdownFormat.assertValidTypedMarkdown(
        proposal.repository(), proposal.mainHead());
    projection.requireMatchingAcceptedTree(
        state.notebook(), state.folders(), state.liveNotes(), proposal.repository(), acceptedHead);
    if (!state.folders().isEmpty() || !state.liveNotes().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, emptyNotebookMessage);
    }
  }

  private String acceptNotesInImpliedRootFolder(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      NotebookGitProposalInitialComposition.NotesInImpliedRootFolder creation) {
    assertReadyEmptyNotebook(
        state,
        proposal,
        acceptedHead,
        "Initial Notes in an implied root Folder require an empty notebook.");

    String folderName =
        creation
            .impliedRootFolderPrefix()
            .substring(0, creation.impliedRootFolderPrefix().length() - 1);
    folderMaterialization.createRootFolderWithoutReadme(
        state.notebook(), creation.notePaths().getFirst(), folderName);
    List<ExportFolderRow> folders = notebookGitStateLoader.foldersOf(state.notebook());
    List<Note> added = applyNotes(state.notebook(), folders, proposal, creation.notePaths());
    projection.requireMatchingAcceptedTree(
        state.notebook(), folders, added, proposal.repository(), proposal.mainHead());
    return bindingPersistence.accept(
        state.binding(), proposal, testabilitySettings.getCurrentUTCTimestamp());
  }

  private String acceptOneNestedFolderReadme(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      NotebookGitProposalInitialComposition.OneNestedFolderReadme creation) {
    String readme = NotebookGitProposalTypedPath.requireReadme(proposal, creation.readmePath());
    assertReadyEmptyNotebook(
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
    assertReadyEmptyNotebook(
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
