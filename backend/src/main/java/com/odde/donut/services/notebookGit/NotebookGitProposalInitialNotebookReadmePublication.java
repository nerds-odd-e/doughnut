package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Note;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.InspectedRegularFile;
import com.odde.donut.testability.TestabilitySettings;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.stereotype.Service;

/** Applies initial root Markdown trees by container and concept role. */
@Service
class NotebookGitProposalInitialNotebookReadmePublication {

  private final NotebookGitProjection projection;
  private final NotebookGitProposalBindingPersistence bindingPersistence;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;
  private final NotebookGitProposalInitialCompositionPublication initialCompositionPublication;

  NotebookGitProposalInitialNotebookReadmePublication(
      NotebookGitProjection projection,
      NotebookGitProposalBindingPersistence bindingPersistence,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      NotebookGitProposalInitialCompositionPublication initialCompositionPublication) {
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
    if (!state.folders().isEmpty()
        || !state.liveNotes().isEmpty()
        || files.isEmpty()
        || files.stream()
            .anyMatch(file -> !isAddedRootNotebookReadme(file) && !isAddedRootMarkdownFile(file))) {
      return Optional.empty();
    }
    initialCompositionPublication.assertReadyEmptyNotebook(
        state, proposal, acceptedHead, "Initial root publication requires an empty notebook.");
    String notebookReadmePath =
        files.stream()
            .filter(NotebookGitProposalInitialNotebookReadmePublication::isAddedRootNotebookReadme)
            .map(InspectedRegularFile::path)
            .findFirst()
            .orElse(null);
    List<String> conceptPaths =
        files.stream()
            .filter(NotebookGitProposalInitialNotebookReadmePublication::isAddedRootMarkdownFile)
            .map(InspectedRegularFile::path)
            .toList();
    return Optional.of(acceptRootTree(state, proposal, notebookReadmePath, conceptPaths));
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

  private String acceptRootTree(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      String notebookReadmePath,
      List<String> conceptPaths) {
    if (notebookReadmePath != null) {
      storeReadme(state, proposal, notebookReadmePath);
    }
    List<Note> notes = new ArrayList<>(conceptPaths.size());
    for (String path : conceptPaths) {
      notes.add(
          initialCompositionPublication.applyConcept(
              state.notebook(), state.folders(), proposal, path));
    }
    entityPersister.flush();
    return acceptMatchingProposedTree(state, proposal, notes);
  }

  private String acceptMatchingProposedTree(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      List<Note> notes) {
    projection.requireMatchingAcceptedTree(
        state.notebook(), state.folders(), notes, proposal.repository(), proposal.mainHead());
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
