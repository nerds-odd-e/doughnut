package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Note;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.ChangedDocument;
import com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.DocumentRole;
import com.odde.donut.testability.TestabilitySettings;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Applies initial Markdown trees by container and concept role. */
@Service
class NotebookGitProposalInitialTreePublication {

  private final NotebookGitProposalFolderMaterialization folderMaterialization;
  private final NotebookGitStateLoader stateLoader;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;
  private final NotebookGitProposalNoteAddition noteAddition;

  NotebookGitProposalInitialTreePublication(
      NotebookGitProposalFolderMaterialization folderMaterialization,
      NotebookGitStateLoader stateLoader,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      NotebookGitProposalNoteAddition noteAddition) {
    this.folderMaterialization = folderMaterialization;
    this.stateLoader = stateLoader;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.noteAddition = noteAddition;
  }

  NotebookGitStateLoader.LockedNotebookState apply(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      List<ChangedDocument> documents) {
    if (documents.isEmpty()
        || documents.stream().anyMatch(document -> !document.path().endsWith(".md"))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Initial publication requires a nonempty Markdown tree.");
    }
    NotebookGitProposalMarkdownFormat.assertValidTypedMarkdown(
        proposal.repository(), proposal.mainHead());
    String notebookReadmePath = null;
    List<String> folderReadmePaths = new ArrayList<>();
    List<String> conceptPaths = new ArrayList<>();
    for (ChangedDocument document : documents) {
      if (document.role() == DocumentRole.CONCEPT) {
        conceptPaths.add(document.path());
      } else if ("README.md".equals(document.path())) {
        notebookReadmePath = document.path();
      } else {
        folderReadmePaths.add(document.path());
      }
    }
    return applyTree(state, proposal, notebookReadmePath, folderReadmePaths, conceptPaths);
  }

  private NotebookGitStateLoader.LockedNotebookState applyTree(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      String notebookReadmePath,
      List<String> folderReadmePaths,
      List<String> conceptPaths) {
    if (notebookReadmePath != null) {
      storeReadme(state, proposal, notebookReadmePath);
    }
    folderMaterialization.materialize(
        state.notebook(),
        state.folders(),
        proposal.repository(),
        ObjectId.fromString(state.binding().getAcceptedGitObjectId()),
        Stream.concat(folderReadmePaths.stream(), conceptPaths.stream()).toList(),
        proposal);
    List<ExportFolderRow> folders = stateLoader.foldersOf(state.notebook());
    List<Note> notes = new ArrayList<>(conceptPaths.size());
    for (String path : conceptPaths) {
      notes.add(
          noteAddition.applyAtProposedPlacement(
              state.notebook(),
              folders,
              proposal,
              path,
              testabilitySettings.getCurrentUTCTimestamp()));
    }
    entityPersister.flush();
    return new NotebookGitStateLoader.LockedNotebookState(
        state.binding(), state.notebook(), folders, notes);
  }

  private void storeReadme(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      String notebookReadmePath) {
    state
        .notebook()
        .setReadmeContent(NotebookGitProposalTypedPath.requireReadme(proposal, notebookReadmePath));
    entityPersister.save(state.notebook());
  }
}
