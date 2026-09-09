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
 * shape, that shape plus one, two, or three added root ordinary Notes, and the exact two-file
 * notebook README plus one root Relationship, on an otherwise empty notebook.
 */
@Service
class NotebookGitProposalInitialNotebookReadmePublication {

  /**
   * Exactly one added root {@code README.md} on an otherwise empty tree: no accepted blob on that
   * path and no other path present (changed or unchanged).
   */
  private record Creation(String notebookReadmePath) {}

  /** Exactly one added root {@code README.md} plus one added root Relationship Markdown file. */
  private record NotebookReadmeWithRootRelationship(
      String notebookReadmePath, String relationshipPath) {}

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
    Optional<NotebookReadmeWithRootRelationship> withRootRelationship =
        findWithRootRelationship(files, proposal);
    if (withRootRelationship.isPresent()) {
      return Optional.of(
          acceptWithRootRelationship(state, proposal, acceptedHead, withRootRelationship.get()));
    }
    Optional<NotebookGitProposalInitialComposition.NotebookReadmeWithRootNotes> withRootNotes =
        findWithRootNotes(files);
    if (withRootNotes.isPresent()) {
      return Optional.of(acceptWithRootNotes(state, proposal, acceptedHead, withRootNotes.get()));
    }
    Optional<Creation> creation = find(files);
    if (creation.isPresent()) {
      return Optional.of(accept(state, proposal, acceptedHead, creation.get()));
    }
    return Optional.empty();
  }

  private static Optional<Creation> find(List<InspectedRegularFile> files) {
    if (files.size() != 1) {
      return Optional.empty();
    }
    InspectedRegularFile file = files.getFirst();
    if (!isAddedRootNotebookReadme(file)) {
      return Optional.empty();
    }
    return Optional.of(new Creation(file.path()));
  }

  private static Optional<NotebookGitProposalInitialComposition.NotebookReadmeWithRootNotes>
      findWithRootNotes(List<InspectedRegularFile> files) {
    if (files.size() < 2 || files.size() > 4) {
      return Optional.empty();
    }
    String notebookReadmePath = null;
    List<String> notePaths = new ArrayList<>(3);
    for (InspectedRegularFile file : files) {
      if (isAddedRootNotebookReadme(file)) {
        if (notebookReadmePath != null) {
          return Optional.empty();
        }
        notebookReadmePath = file.path();
      } else if (isAddedRootMarkdownFile(file)) {
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

  private static Optional<NotebookReadmeWithRootRelationship> findWithRootRelationship(
      List<InspectedRegularFile> files, NotebookGitProposalImporter.ImportedProposal proposal) {
    if (files.size() != 2) {
      return Optional.empty();
    }
    String notebookReadmePath = null;
    String relationshipPath = null;
    for (InspectedRegularFile file : files) {
      if (isAddedRootNotebookReadme(file)) {
        if (notebookReadmePath != null) {
          return Optional.empty();
        }
        notebookReadmePath = file.path();
      } else if (isAddedRootMarkdownFile(file)
          && NotebookGitProposalTypedPath.authoredTypeEquals(
              proposal, file.path(), "Relationship")) {
        if (relationshipPath != null) {
          return Optional.empty();
        }
        relationshipPath = file.path();
      } else {
        return Optional.empty();
      }
    }
    if (notebookReadmePath == null || relationshipPath == null) {
      return Optional.empty();
    }
    return Optional.of(
        new NotebookReadmeWithRootRelationship(notebookReadmePath, relationshipPath));
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

  private String accept(
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
    return acceptMatchingProposedTree(state, proposal, state.liveNotes());
  }

  private String acceptWithRootNotes(
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
    return acceptMatchingProposedTree(
        state,
        proposal,
        initialCompositionPublication.applyNotes(
            state.notebook(), state.folders(), proposal, creation.notePaths()));
  }

  private String acceptWithRootRelationship(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      NotebookReadmeWithRootRelationship creation) {
    storeOnEmptyNotebook(
        state,
        proposal,
        acceptedHead,
        creation.notebookReadmePath(),
        "Initial notebook Readme and root Relationship require an empty notebook.");
    return acceptMatchingProposedTree(
        state,
        proposal,
        List.of(
            initialCompositionPublication.applyRelationship(
                state.notebook(), state.folders(), proposal, creation.relationshipPath())));
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
    state
        .notebook()
        .setReadmeContent(NotebookGitProposalTypedPath.requireReadme(proposal, notebookReadmePath));
    entityPersister.save(state.notebook());
  }
}
