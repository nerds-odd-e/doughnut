package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.notebookExport.ExportFolderRow;
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
  private final TestabilitySettings testabilitySettings;
  private final NotebookGitProposalFolderRelocation folderRelocation;
  private final NotebookGitProposalDocumentApplication documentApplication;
  private final NotebookGitProposalNoteAddition noteAddition;
  private final NotebookGitProposalOrdinaryNoteApplication ordinaryNoteApplication;

  public NotebookGitProposalPublisher(
      NotebookGitStateLoader notebookGitStateLoader,
      AuthorizationService authorizationService,
      NotebookGitProjection projection,
      NotebookGitProposalAcceptance proposalAcceptance,
      TestabilitySettings testabilitySettings,
      NotebookGitProposalFolderRelocation folderRelocation,
      NotebookGitProposalDocumentApplication documentApplication,
      NotebookGitProposalNoteAddition noteAddition,
      NotebookGitProposalOrdinaryNoteApplication ordinaryNoteApplication) {
    this.notebookGitStateLoader = notebookGitStateLoader;
    this.authorizationService = authorizationService;
    this.projection = projection;
    this.proposalAcceptance = proposalAcceptance;
    this.testabilitySettings = testabilitySettings;
    this.folderRelocation = folderRelocation;
    this.documentApplication = documentApplication;
    this.noteAddition = noteAddition;
    this.ordinaryNoteApplication = ordinaryNoteApplication;
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
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Notebook has no Git binding."));
    NotebookGitBinding binding = state.binding();
    Notebook notebook = state.notebook();
    List<ExportFolderRow> folders = state.folders();
    List<Note> liveNotes = state.liveNotes();
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

    List<NotebookGitProposalTreeShape.InspectedRegularFile> files =
        NotebookGitProposalTreeShape.inspectRegularFiles(
            proposal.repository(), acceptedHead, proposal.mainHead());
    List<NotebookGitProposalTreeShape.ChangedDocument> documents =
        NotebookGitProposalTreeShape.classifyChangedDocuments(files);
    Timestamp publishedAt = testabilitySettings.getCurrentUTCTimestamp();
    boolean emptyAcceptedNotebook = isEmptyAcceptedNotebook(folders, liveNotes, files);
    if (emptyAcceptedNotebook
        && (documents.isEmpty()
            || documents.stream().anyMatch(document -> !document.path().endsWith(".md")))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Initial publication requires a nonempty Markdown tree.");
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
      admitted = NotebookGitProposalTreeShape.requireAdmittedResidualShape(documents);
      List<NotebookGitProposalTreeShape.ChangedDocument> beforeRelocation = new ArrayList<>();
      List<NotebookGitProposalTreeShape.ChangedDocument> afterRelocation = new ArrayList<>();
      partitionAroundRelocation(
          admitted.additions(), relocation.get(), beforeRelocation, afterRelocation);
      published = state;
      if (!beforeRelocation.isEmpty()) {
        projection.requireMatchingAcceptedTree(
            notebook, folders, liveNotes, proposal.repository(), acceptedHead);
        published = documentApplication.apply(published, proposal, beforeRelocation, publishedAt);
        published =
            folderRelocation.applyAfterMatchedAcceptedTree(
                published, proposal, acceptedHead, relocation.get());
      } else {
        published = folderRelocation.apply(published, proposal, acceptedHead, relocation.get());
      }
      if (!afterRelocation.isEmpty()) {
        published =
            applyAdditionsUnderRelocatedDestination(
                published, proposal, afterRelocation, publishedAt);
      }
    } else {
      admitted =
          NotebookGitProposalTreeShape.requireAdmittedShape(
              proposal.repository(), acceptedHead, proposal.mainHead(), documents);
      if (admitted.noteChanges().isEmpty() && admitted.additions().isEmpty()) {
        projection.requireMatchingAcceptedTree(
            notebook, folders, liveNotes, proposal.repository(), acceptedHead);
        return proposalAcceptance.acceptMatchingProposedTree(state, proposal, publishedAt);
      }
      NotebookGitProposalMarkdownFormat.assertValidTypedMarkdown(
          proposal.repository(), proposal.mainHead());
      projection.requireMatchingAcceptedTree(
          notebook, folders, liveNotes, proposal.repository(), acceptedHead);
      published =
          new NotebookGitStateLoader.LockedNotebookState(binding, notebook, folders, liveNotes);
    }
    List<Note> proposedLiveNotes = new ArrayList<>(published.liveNotes());
    List<ExportFolderRow> proposedFolders = published.folders();
    // Deletions before additions so same-path deletion-gap recreation can replace the old identity.
    ordinaryNoteApplication.applyDeletions(admitted, proposedFolders, proposedLiveNotes);
    if (relocation.isEmpty() && !admitted.additions().isEmpty()) {
      published =
          documentApplication.apply(
              new NotebookGitStateLoader.LockedNotebookState(
                  published.binding(), published.notebook(), proposedFolders, proposedLiveNotes),
              proposal,
              admitted.additions(),
              publishedAt);
      proposedLiveNotes = new ArrayList<>(published.liveNotes());
      proposedFolders = published.folders();
    }
    ordinaryNoteApplication.applyModificationsAndRenames(
        admitted, proposedFolders, proposal, acceptedHead, proposedLiveNotes, publishedAt);
    return proposalAcceptance.acceptMatchingProposedTree(
        new NotebookGitStateLoader.LockedNotebookState(
            published.binding(), published.notebook(), proposedFolders, proposedLiveNotes),
        proposal,
        publishedAt);
  }

  /**
   * Container or note additions under the relocated destination need the source Folder reparented
   * first; additions that create or fill the destination parent must run before reparenting.
   */
  private static void partitionAroundRelocation(
      List<NotebookGitProposalTreeShape.ChangedDocument> additions,
      NotebookGitProposalFolderShape.FolderRelocation relocation,
      List<NotebookGitProposalTreeShape.ChangedDocument> beforeRelocation,
      List<NotebookGitProposalTreeShape.ChangedDocument> afterRelocation) {
    String destPrefix = relocation.destPrefix();
    for (NotebookGitProposalTreeShape.ChangedDocument addition : additions) {
      if (addition.path().startsWith(destPrefix + "/")) {
        afterRelocation.add(addition);
      } else {
        beforeRelocation.add(addition);
      }
    }
  }

  /**
   * Tip concept notes under an already-reparented destination resolve against live folders and tip
   * representation; they must not re-materialize the relocated ancestry from the accepted tree.
   */
  private NotebookGitStateLoader.LockedNotebookState applyAdditionsUnderRelocatedDestination(
      NotebookGitStateLoader.LockedNotebookState published,
      NotebookGitProposalImporter.ImportedProposal proposal,
      List<NotebookGitProposalTreeShape.ChangedDocument> additions,
      Timestamp publishedAt) {
    ObjectId acceptedHead = ObjectId.fromString(published.binding().getAcceptedGitObjectId());
    List<Note> notes = new ArrayList<>(published.liveNotes());
    List<NotebookGitProposalTreeShape.ChangedDocument> containers = new ArrayList<>();
    for (NotebookGitProposalTreeShape.ChangedDocument addition : additions) {
      if (addition.role() == NotebookGitProposalTreeShape.DocumentRole.CONTAINER) {
        containers.add(addition);
        continue;
      }
      notes.add(
          noteAddition.applyAtRepresentedPath(
              published.notebook(),
              published.folders(),
              proposal,
              acceptedHead,
              addition.path(),
              publishedAt));
    }
    NotebookGitStateLoader.LockedNotebookState withNotes =
        new NotebookGitStateLoader.LockedNotebookState(
            published.binding(), published.notebook(), published.folders(), notes);
    if (containers.isEmpty()) {
      return withNotes;
    }
    return documentApplication.apply(withNotes, proposal, containers, publishedAt);
  }

  private static boolean isEmptyAcceptedNotebook(
      List<ExportFolderRow> folders,
      List<Note> liveNotes,
      List<NotebookGitProposalTreeShape.InspectedRegularFile> files) {
    return folders.isEmpty()
        && liveNotes.isEmpty()
        && files.stream().allMatch(file -> file.acceptedBlobId() == null);
  }
}
