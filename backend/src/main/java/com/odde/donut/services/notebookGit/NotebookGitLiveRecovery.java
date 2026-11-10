package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthoredNoteDocumentPersistence;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.testability.TestabilitySettings;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Accepts explicitly reviewed live Portable edits without replacing private identities. */
@Service
public class NotebookGitLiveRecovery {
  private final NotebookGitStateLoader stateLoader;
  private final NotebookGitAcceptedRepositoryStore repositoryStore;
  private final NotebookGitProjection projection;
  private final AuthorizationService authorizationService;
  private final NotebookGitCandidateAdmission candidateAdmission;
  private final NotebookGitProposalAcceptance proposalAcceptance;
  private final NotebookGitNoteDocuments noteDocuments;
  private final AuthoredNoteDocumentPersistence documentPersistence;
  private final TestabilitySettings testabilitySettings;

  public NotebookGitLiveRecovery(
      NotebookGitStateLoader stateLoader,
      NotebookGitAcceptedRepositoryStore repositoryStore,
      NotebookGitProjection projection,
      AuthorizationService authorizationService,
      NotebookGitCandidateAdmission candidateAdmission,
      NotebookGitProposalAcceptance proposalAcceptance,
      NotebookGitNoteDocuments noteDocuments,
      AuthoredNoteDocumentPersistence documentPersistence,
      TestabilitySettings testabilitySettings) {
    this.stateLoader = stateLoader;
    this.repositoryStore = repositoryStore;
    this.projection = projection;
    this.authorizationService = authorizationService;
    this.candidateAdmission = candidateAdmission;
    this.proposalAcceptance = proposalAcceptance;
    this.noteDocuments = noteDocuments;
    this.documentPersistence = documentPersistence;
    this.testabilitySettings = testabilitySettings;
  }

  @Transactional(
      propagation = Propagation.REQUIRES_NEW,
      isolation = Isolation.SERIALIZABLE,
      rollbackFor = Exception.class)
  public String recover(Integer notebookId, Review review) throws UnexpectedNoAccessRightException {
    var state =
        stateLoader
            .findByNotebookIdForUpdate(notebookId)
            .orElseThrow(NotebookGitBindingMissing::refusal);
    authorizationService.assertAuthorization(state.notebook());
    if (!state.binding().getAcceptedGitObjectId().equals(review.acceptedHead())) {
      throw staleReview();
    }
    try (var accepted = repositoryStore.open(state.binding())) {
      var live =
          projection.liveTree(
              state.notebook(),
              state.folders(),
              state.storedNotes(),
              accepted.repository(),
              accepted.head());
      if (!live.treeId().name().equals(review.liveTree())) {
        throw staleReview();
      }
      var publishedAt = testabilitySettings.getCurrentUTCTimestamp();
      ObjectId candidate =
          NotebookGitCommitBuilder.append(
              accepted.repository(),
              accepted.head(),
              live,
              NotebookGitCommitBuilder.SYSTEM_AUTHOR_NAME,
              NotebookGitCommitBuilder.SYSTEM_AUTHOR_EMAIL,
              "Preserve reviewed live notebook edits",
              publishedAt.toInstant());
      var proposal =
          new NotebookGitProposalImporter.ImportedProposal(accepted.repository(), candidate);
      var files = candidateAdmission.admit(state.binding(), accepted.repository(), candidate);
      var noteChanges =
          NotebookGitProposalTreeShape.noteChangesFrom(
              NotebookGitProposalTreeShape.classifyChangedDocuments(files).stream()
                  .filter(document -> PortablePathKind.isNote(document.path()))
                  .toList());
      NotebookGitProposalNoteCorrespondence.admitOrdinaryNoteChanges(
          accepted.repository(), accepted.head(), candidate, noteChanges);
      for (var change : noteChanges) {
        if (change.kind() != NotebookGitProposalTreeShape.ChangeKind.DELETED) {
          Note note = projection.requireOneNoteAtPath(state.storedNotes(), change.path());
          documentPersistence.persist(
              note,
              noteDocuments.readValidatedDocument(proposal, change.path()),
              note.getUpdatedAt());
        }
      }
      return proposalAcceptance.acceptMatchingProposedTree(state, proposal, publishedAt);
    }
  }

  private static ResponseStatusException staleReview() {
    return new ResponseStatusException(
        HttpStatus.CONFLICT,
        "The notebook changed since this review. Run \"donut notebook diagnose\" and review the current accepted and live state before recovery.");
  }

  public record Review(String acceptedHead, String liveTree) {}
}
