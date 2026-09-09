package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.InspectedRegularFile;
import com.odde.donut.testability.TestabilitySettings;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Recognizes and accepts the sole-added-root-{@code README.md} initial notebook Readme proposal
 * shape on an otherwise empty notebook.
 */
@Service
class NotebookGitProposalInitialNotebookReadmePublication {

  /**
   * Exactly one added root {@code README.md} on an otherwise empty tree: no accepted blob on that
   * path and no other path present (changed or unchanged).
   */
  record Creation(String notebookReadmePath) {}

  private final NotebookGitProjection projection;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;

  NotebookGitProposalInitialNotebookReadmePublication(
      NotebookGitProjection projection,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings) {
    this.projection = projection;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
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

  static boolean isAddedRootNotebookReadme(InspectedRegularFile file) {
    return file.acceptedBlobId() == null
        && file.proposedBlobId() != null
        && "README.md".equals(file.path());
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
    return acceptBinding(state.binding(), proposal);
  }

  void storeOnEmptyNotebook(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      String notebookReadmePath,
      String emptyNotebookMessage) {
    NotebookGitProposalMarkdownFormat.assertValidTypedMarkdown(
        proposal.repository(), proposal.mainHead());
    projection.requireMatchingAcceptedTree(
        state.notebook(), state.folders(), state.liveNotes(), proposal.repository(), acceptedHead);
    if (!state.folders().isEmpty() || !state.liveNotes().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, emptyNotebookMessage);
    }

    state
        .notebook()
        .setReadmeContent(NotebookGitProposalTypedPath.requireReadme(proposal, notebookReadmePath));
    entityPersister.save(state.notebook());
  }

  private String acceptBinding(
      NotebookGitBinding binding, NotebookGitProposalImporter.ImportedProposal proposal) {
    Timestamp publishedAt = testabilitySettings.getCurrentUTCTimestamp();
    NotebookGitBundleWriter.BundleWriteResult written =
        NotebookGitBundleWriter.write(proposal.repository());
    binding.setAcceptedGitObjectId(written.headObjectId());
    binding.setBundleBytes(written.bundleBytes());
    binding.setUpdatedAt(publishedAt);
    entityPersister.save(binding);
    return written.headObjectId();
  }
}
