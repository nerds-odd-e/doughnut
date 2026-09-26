package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import com.odde.donut.services.notebookTree.PortableTreeFolderRow;
import com.odde.donut.testability.TestabilitySettings;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotebookGitProposalPublisher {

  private final NotebookGitStateLoader notebookGitStateLoader;
  private final AuthorizationService authorizationService;
  private final NotebookGitProjection projection;
  private final NotebookGitProposalAcceptance proposalAcceptance;
  private final NotebookGitAcceptedRepositoryStore repositoryStore;
  private final TestabilitySettings testabilitySettings;
  private final NotebookGitProposalFolderRelocation folderRelocation;
  private final NotebookGitProposalDocumentApplication documentApplication;
  private final NotebookGitProposalNoteAddition noteAddition;
  private final NotebookGitProposalOrdinaryNoteApplication ordinaryNoteApplication;
  private final NotebookAttachmentContent notebookAttachmentContent;
  private final NotebookGitBookSourceFileProtection bookSourceFileProtection;

  public NotebookGitProposalPublisher(
      NotebookGitStateLoader notebookGitStateLoader,
      AuthorizationService authorizationService,
      NotebookGitProjection projection,
      NotebookGitProposalAcceptance proposalAcceptance,
      NotebookGitAcceptedRepositoryStore repositoryStore,
      TestabilitySettings testabilitySettings,
      NotebookGitProposalFolderRelocation folderRelocation,
      NotebookGitProposalDocumentApplication documentApplication,
      NotebookGitProposalNoteAddition noteAddition,
      NotebookGitProposalOrdinaryNoteApplication ordinaryNoteApplication,
      NotebookAttachmentContent notebookAttachmentContent,
      NotebookGitBookSourceFileProtection bookSourceFileProtection) {
    this.notebookGitStateLoader = notebookGitStateLoader;
    this.authorizationService = authorizationService;
    this.projection = projection;
    this.proposalAcceptance = proposalAcceptance;
    this.repositoryStore = repositoryStore;
    this.testabilitySettings = testabilitySettings;
    this.folderRelocation = folderRelocation;
    this.documentApplication = documentApplication;
    this.noteAddition = noteAddition;
    this.ordinaryNoteApplication = ordinaryNoteApplication;
    this.notebookAttachmentContent = notebookAttachmentContent;
    this.bookSourceFileProtection = bookSourceFileProtection;
  }

  @Transactional(
      propagation = Propagation.REQUIRES_NEW,
      isolation = Isolation.SERIALIZABLE,
      rollbackFor = Exception.class)
  public String publish(
      Integer notebookId,
      String expectedHead,
      NotebookGitProposalImporter.ImportedProposal proposal)
      throws UnexpectedNoAccessRightException {
    NotebookGitStateLoader.LockedNotebookState state =
        notebookGitStateLoader
            .findByNotebookIdForUpdate(notebookId)
            .orElseThrow(NotebookGitBindingMissing::refusal);
    NotebookGitBinding binding = state.binding();
    Notebook notebook = state.notebook();
    List<PortableTreeFolderRow> folders = state.folders();
    List<Note> storedNotes = state.storedNotes();
    authorizationService.assertAuthorization(notebook);
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    if (proposal.mainHead().equals(acceptedHead)) {
      proposalAcceptance.requireMatchingProposedTree(state, proposal);
      return binding.getAcceptedGitObjectId();
    }
    if (!expectedHead.equals(binding.getAcceptedGitObjectId())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "expectedHead no longer matches the notebook's current accepted head.");
    }
    NotebookGitProposalAncestry.assertFollowsAcceptedHead(
        proposal.repository(), proposal.mainHead(), acceptedHead);
    try (var accepted = repositoryStore.open(binding)) {
      NotebookGitAttachmentSizeAdmission.admit(
          proposal.repository(),
          proposal.mainHead(),
          accepted.repository(),
          acceptedHead,
          notebook.getId(),
          notebookAttachmentContent);
    }

    List<NotebookGitProposalTreeShape.InspectedRegularFile> files =
        NotebookGitProposalTreeShape.inspectRegularFiles(
            proposal.repository(), acceptedHead, proposal.mainHead());
    bookSourceFileProtection.refuseChanging(notebook.getId(), files);
    List<NotebookGitProposalTreeShape.ChangedDocument> documents =
        NotebookGitProposalTreeShape.classifyChangedDocuments(files);
    Timestamp publishedAt = testabilitySettings.getCurrentUTCTimestamp();
    boolean emptyAcceptedNotebook = isEmptyAcceptedNotebook(folders, storedNotes, files);
    if (emptyAcceptedNotebook
        && documents.stream()
            .noneMatch(
                document -> NotebookGitProposalTreeShape.carriesPortableContent(document.path()))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Initial publication requires nonempty Portable content.");
    }
    Optional<NotebookGitProposalFolderShape.FolderRelocation> relocation =
        documents.isEmpty()
            ? Optional.empty()
            : NotebookGitProposalFolderShape.requireExactOrCarried(
                proposal.repository(), acceptedHead, proposal.mainHead(), files);
    NotebookGitStateLoader.LockedNotebookState published;
    final NotebookGitProposalTreeShape.AdmittedShape admitted;
    if (relocation.isPresent()) {
      documents =
          NotebookGitProposalTreeShape.classifyChangedDocuments(
              NotebookGitProposalFolderShape.residualOutside(files, relocation.get()));
      if (documents.isEmpty()) {
        published = folderRelocation.apply(state, proposal, acceptedHead, relocation.get());
        return proposalAcceptance.acceptMatchingProposedTree(published, proposal);
      }
      admitted =
          NotebookGitProposalTreeShape.requireAdmittedResidualShape(
              proposal.repository(), documents);
      List<NotebookGitProposalTreeShape.ChangedDocument> beforeRelocation = new ArrayList<>();
      List<NotebookGitProposalTreeShape.ChangedDocument> afterRelocation = new ArrayList<>();
      NotebookGitProposalRelocatedDocuments.partitionAroundRelocation(
          admitted.documents(), relocation.get(), beforeRelocation, afterRelocation);
      published = state;
      if (!beforeRelocation.isEmpty()) {
        projection.requireMatchingAcceptedTree(
            notebook, folders, storedNotes, proposal.repository(), acceptedHead);
        published = documentApplication.apply(published, proposal, beforeRelocation, publishedAt);
        published =
            folderRelocation.applyAfterMatchedAcceptedTree(
                published, proposal, acceptedHead, relocation.get());
      } else {
        published = folderRelocation.apply(published, proposal, acceptedHead, relocation.get());
      }
      if (!afterRelocation.isEmpty()) {
        published =
            NotebookGitProposalRelocatedDocuments.applyUnderDestination(
                published,
                proposal,
                afterRelocation,
                publishedAt,
                noteAddition,
                documentApplication);
      }
    } else {
      admitted =
          NotebookGitProposalTreeShape.requireAdmittedShape(
              proposal.repository(), acceptedHead, proposal.mainHead(), documents);
      if (admitted.noteChanges().isEmpty() && admitted.documents().isEmpty()) {
        projection.requireMatchingAcceptedTree(
            notebook, folders, storedNotes, proposal.repository(), acceptedHead);
        return proposalAcceptance.acceptMatchingProposedTree(state, proposal, publishedAt);
      }
      NotebookGitProposalMarkdownFormat.assertValidTypedMarkdown(
          proposal.repository(), proposal.mainHead());
      projection.requireMatchingAcceptedTree(
          notebook, folders, storedNotes, proposal.repository(), acceptedHead);
      published =
          new NotebookGitStateLoader.LockedNotebookState(binding, notebook, folders, storedNotes);
    }
    List<Note> proposedNotes = new ArrayList<>(published.storedNotes());
    // Deletions before additions so same-path deletion-gap recreation can replace the old identity.
    ordinaryNoteApplication.applyDeletions(admitted, proposedNotes);
    if (relocation.isEmpty() && !admitted.documents().isEmpty()) {
      published =
          documentApplication.apply(
              new NotebookGitStateLoader.LockedNotebookState(
                  published.binding(), published.notebook(), published.folders(), proposedNotes),
              proposal,
              admitted.documents(),
              publishedAt);
      proposedNotes = new ArrayList<>(published.storedNotes());
    }
    List<PortableTreeFolderRow> proposedFolders =
        ordinaryNoteApplication.applyModificationsAndRenames(
            admitted, published.notebook(), proposal, proposedNotes, publishedAt);
    return proposalAcceptance.acceptMatchingProposedTree(
        new NotebookGitStateLoader.LockedNotebookState(
            published.binding(), published.notebook(), proposedFolders, proposedNotes),
        proposal,
        publishedAt);
  }

  private static boolean isEmptyAcceptedNotebook(
      List<PortableTreeFolderRow> folders,
      List<Note> storedNotes,
      List<NotebookGitProposalTreeShape.InspectedRegularFile> files) {
    return folders.isEmpty()
        && storedNotes.isEmpty()
        && files.stream().allMatch(file -> file.acceptedBlobId() == null);
  }
}
