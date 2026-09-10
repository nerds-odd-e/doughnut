package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.InspectedRegularFile;
import com.odde.donut.testability.TestabilitySettings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Applies initial Markdown trees by container and concept role. */
@Service
class NotebookGitProposalInitialTreePublication {

  private final NotebookGitProposalFolderMaterialization folderMaterialization;
  private final NotebookGitStateLoader stateLoader;
  private final NotebookGitProjection projection;
  private final NotebookGitProposalBindingPersistence bindingPersistence;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;
  private final NotebookGitProposalNoteAddition noteAddition;

  NotebookGitProposalInitialTreePublication(
      NotebookGitProposalFolderMaterialization folderMaterialization,
      NotebookGitStateLoader stateLoader,
      NotebookGitProjection projection,
      NotebookGitProposalBindingPersistence bindingPersistence,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      NotebookGitProposalNoteAddition noteAddition) {
    this.folderMaterialization = folderMaterialization;
    this.stateLoader = stateLoader;
    this.projection = projection;
    this.bindingPersistence = bindingPersistence;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.noteAddition = noteAddition;
  }

  String accept(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      List<InspectedRegularFile> files) {
    if (files.isEmpty() || files.stream().anyMatch(file -> !file.path().endsWith(".md"))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Initial publication requires a nonempty Markdown tree.");
    }
    NotebookGitProposalMarkdownFormat.assertValidTypedMarkdown(
        proposal.repository(), proposal.mainHead());
    List<String> paths = files.stream().map(InspectedRegularFile::path).toList();
    return acceptTree(
        state,
        proposal,
        paths.contains("README.md") ? "README.md" : null,
        paths.stream().filter(path -> path.endsWith("/README.md")).toList(),
        paths.stream()
            .filter(path -> !path.equals("README.md") && !path.endsWith("/README.md"))
            .toList());
  }

  private String acceptTree(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      String notebookReadmePath,
      List<String> folderReadmePaths,
      List<String> conceptPaths) {
    if (notebookReadmePath != null) {
      storeReadme(state, proposal, notebookReadmePath);
    }
    Map<String, Folder> createdFolders =
        folderMaterialization.createFolderAncestry(
            state.notebook(),
            Stream.concat(folderReadmePaths.stream(), conceptPaths.stream()).toList());
    for (String path : folderReadmePaths) {
      Folder folder = createdFolders.get(path.substring(0, path.lastIndexOf('/')));
      folder.setReadmeContent(NotebookGitProposalTypedPath.requireReadme(proposal, path));
      entityPersister.save(folder);
    }
    entityPersister.flush();
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
    return acceptMatchingProposedTree(state, proposal, folders, notes);
  }

  private String acceptMatchingProposedTree(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      List<ExportFolderRow> folders,
      List<Note> notes) {
    projection.requireMatchingAcceptedTree(
        state.notebook(), folders, notes, proposal.repository(), proposal.mainHead());
    return bindingPersistence.accept(
        state.binding(), proposal, testabilitySettings.getCurrentUTCTimestamp());
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
