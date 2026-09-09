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

/**
 * Recognizes and accepts the sole-added-root-{@code README.md} initial notebook Readme proposal
 * shape, and that shape plus one or two added root ordinary Notes, on an otherwise empty notebook.
 */
@Service
class NotebookGitProposalInitialNotebookReadmePublication {

  /**
   * Exactly one added root {@code README.md} on an otherwise empty tree: no accepted blob on that
   * path and no other path present (changed or unchanged).
   */
  record Creation(String notebookReadmePath) {}

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

  static Optional<Creation> find(List<InspectedRegularFile> files) {
    if (files.size() != 1) {
      return Optional.empty();
    }
    InspectedRegularFile file = files.getFirst();
    if (!isAddedRootNotebookReadme(file)) {
      return Optional.empty();
    }
    return Optional.of(new Creation(file.path()));
  }

  static Optional<NotebookGitProposalInitialComposition.NotebookReadmeWithRootNotes>
      findWithRootNotes(List<InspectedRegularFile> files) {
    if (files.size() < 2 || files.size() > 3) {
      return Optional.empty();
    }
    String notebookReadmePath = null;
    List<String> notePaths = new ArrayList<>(2);
    for (InspectedRegularFile file : files) {
      if (isAddedRootNotebookReadme(file)) {
        if (notebookReadmePath != null) {
          return Optional.empty();
        }
        notebookReadmePath = file.path();
      } else if (isAddedRootOrdinaryNote(file)) {
        notePaths.add(file.path());
      } else {
        return Optional.empty();
      }
    }
    if (notebookReadmePath == null || notePaths.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        new NotebookGitProposalInitialComposition.NotebookReadmeWithRootNotes(
            notebookReadmePath, notePaths));
  }

  static boolean isAddedRootNotebookReadme(InspectedRegularFile file) {
    return file.acceptedBlobId() == null
        && file.proposedBlobId() != null
        && "README.md".equals(file.path());
  }

  static boolean isAddedRootOrdinaryNote(InspectedRegularFile file) {
    String path = file.path();
    return file.acceptedBlobId() == null
        && file.proposedBlobId() != null
        && path.indexOf('/') < 0
        && path.endsWith(".md")
        && !"README.md".equals(path);
  }

  String accept(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      Creation creation) {
    storeOnEmptyNotebook(
        state,
        proposal,
        acceptedHead,
        creation.notebookReadmePath(),
        "Initial notebook Readme requires an empty notebook.");
    entityPersister.flush();
    projection.requireMatchingAcceptedTree(
        state.notebook(),
        state.folders(),
        state.liveNotes(),
        proposal.repository(),
        proposal.mainHead());
    return bindingPersistence.accept(
        state.binding(), proposal, testabilitySettings.getCurrentUTCTimestamp());
  }

  String acceptWithRootNotes(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      NotebookGitProposalInitialComposition.NotebookReadmeWithRootNotes creation) {
    storeOnEmptyNotebook(
        state,
        proposal,
        acceptedHead,
        creation.notebookReadmePath(),
        "Initial notebook Readme and root Notes require an empty notebook.");
    List<Note> added =
        initialCompositionPublication.applyNotes(
            state.notebook(), state.folders(), proposal, creation.notePaths());
    projection.requireMatchingAcceptedTree(
        state.notebook(), state.folders(), added, proposal.repository(), proposal.mainHead());
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
    state
        .notebook()
        .setReadmeContent(NotebookGitProposalTypedPath.requireReadme(proposal, notebookReadmePath));
    entityPersister.save(state.notebook());
  }
}
