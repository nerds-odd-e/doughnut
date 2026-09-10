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
import java.util.Optional;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.stereotype.Service;

/** Applies initial Markdown trees by container and concept role. */
@Service
class NotebookGitProposalInitialTreePublication {

  private final NotebookGitProposalFolderMaterialization folderMaterialization;
  private final NotebookGitStateLoader stateLoader;
  private final NotebookGitProjection projection;
  private final NotebookGitProposalBindingPersistence bindingPersistence;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;
  private final NotebookGitProposalInitialCompositionPublication initialCompositionPublication;

  NotebookGitProposalInitialTreePublication(
      NotebookGitProposalFolderMaterialization folderMaterialization,
      NotebookGitStateLoader stateLoader,
      NotebookGitProjection projection,
      NotebookGitProposalBindingPersistence bindingPersistence,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      NotebookGitProposalInitialCompositionPublication initialCompositionPublication) {
    this.folderMaterialization = folderMaterialization;
    this.stateLoader = stateLoader;
    this.projection = projection;
    this.bindingPersistence = bindingPersistence;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.initialCompositionPublication = initialCompositionPublication;
  }

  Optional<String> tryAccept(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      List<InspectedRegularFile> files) {
    if ((!state.folders().isEmpty() || !state.liveNotes().isEmpty())
        && files.stream()
            .anyMatch(NotebookGitProposalInitialTreePublication::isAddedRootNotebookReadme)
        && files.stream().allMatch(NotebookGitProposalInitialTreePublication::isAddedReadme)) {
      initialCompositionPublication.assertReadyEmptyNotebook(
          state,
          proposal,
          acceptedHead,
          "Initial container publication requires an empty notebook.");
    }
    if (!state.folders().isEmpty()
        || !state.liveNotes().isEmpty()
        || files.isEmpty()
        || !(files.stream().allMatch(NotebookGitProposalInitialTreePublication::isAddedReadme)
            || files.stream()
                .allMatch(
                    file -> isAddedRootNotebookReadme(file) || isAddedRootMarkdownFile(file)))) {
      return Optional.empty();
    }
    initialCompositionPublication.assertReadyEmptyNotebook(
        state, proposal, acceptedHead, "Initial root publication requires an empty notebook.");
    String notebookReadmePath =
        files.stream()
            .filter(NotebookGitProposalInitialTreePublication::isAddedRootNotebookReadme)
            .map(InspectedRegularFile::path)
            .findFirst()
            .orElse(null);
    List<String> conceptPaths =
        files.stream()
            .filter(NotebookGitProposalInitialTreePublication::isAddedRootMarkdownFile)
            .map(InspectedRegularFile::path)
            .toList();
    List<String> folderReadmePaths =
        files.stream()
            .filter(file -> isAddedReadme(file) && !isAddedRootNotebookReadme(file))
            .map(InspectedRegularFile::path)
            .toList();
    return Optional.of(
        acceptTree(state, proposal, notebookReadmePath, folderReadmePaths, conceptPaths));
  }

  private static boolean isAddedReadme(InspectedRegularFile file) {
    return file.acceptedBlobId() == null
        && file.proposedBlobId() != null
        && ("README.md".equals(file.path()) || file.path().endsWith("/README.md"));
  }

  static boolean isAddedRootNotebookReadme(InspectedRegularFile file) {
    return file.acceptedBlobId() == null
        && file.proposedBlobId() != null
        && "README.md".equals(file.path());
  }

  static boolean isAddedRootMarkdownFile(InspectedRegularFile file) {
    String path = file.path();
    return file.acceptedBlobId() == null
        && file.proposedBlobId() != null
        && path.indexOf('/') < 0
        && path.endsWith(".md")
        && !"README.md".equals(path);
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
        folderMaterialization.createFolderAncestry(state.notebook(), folderReadmePaths);
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
          initialCompositionPublication.applyConcept(state.notebook(), folders, proposal, path));
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

  void storeOnEmptyNotebook(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      String notebookReadmePath,
      String emptyNotebookMessage) {
    initialCompositionPublication.assertReadyEmptyNotebook(
        state, proposal, acceptedHead, emptyNotebookMessage);
    storeReadme(state, proposal, notebookReadmePath);
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
