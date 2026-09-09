package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.AuthoredNoteDocumentPersistence;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.NoteService;
import com.odde.donut.services.NoteTitlePlacementRules;
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
  private final NotebookGitProposalBindingPersistence bindingPersistence;
  private final AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence;
  private final TestabilitySettings testabilitySettings;
  private final EntityPersister entityPersister;
  private final NotebookGitProposalFilenameTitle filenameTitle;
  private final NoteService noteService;
  private final NoteTitlePlacementRules noteTitlePlacementRules;
  private final NotebookGitProposalFolderAcceptance folderAcceptance;
  private final NotebookGitProposalInitialNotebookReadmePublication
      initialNotebookReadmePublication;
  private final NotebookGitProposalInitialCompositionPublication initialCompositionPublication;
  private final NotebookGitProposalNoteAddition noteAddition;

  public NotebookGitProposalPublisher(
      NotebookGitStateLoader notebookGitStateLoader,
      AuthorizationService authorizationService,
      NotebookGitProjection projection,
      NotebookGitProposalBindingPersistence bindingPersistence,
      AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence,
      TestabilitySettings testabilitySettings,
      EntityPersister entityPersister,
      NotebookGitProposalFilenameTitle filenameTitle,
      NoteService noteService,
      NoteTitlePlacementRules noteTitlePlacementRules,
      NotebookGitProposalFolderAcceptance folderAcceptance,
      NotebookGitProposalInitialNotebookReadmePublication initialNotebookReadmePublication,
      NotebookGitProposalInitialCompositionPublication initialCompositionPublication,
      NotebookGitProposalNoteAddition noteAddition) {
    this.notebookGitStateLoader = notebookGitStateLoader;
    this.authorizationService = authorizationService;
    this.projection = projection;
    this.bindingPersistence = bindingPersistence;
    this.authoredNoteDocumentPersistence = authoredNoteDocumentPersistence;
    this.testabilitySettings = testabilitySettings;
    this.entityPersister = entityPersister;
    this.filenameTitle = filenameTitle;
    this.noteService = noteService;
    this.noteTitlePlacementRules = noteTitlePlacementRules;
    this.folderAcceptance = folderAcceptance;
    this.initialNotebookReadmePublication = initialNotebookReadmePublication;
    this.initialCompositionPublication = initialCompositionPublication;
    this.noteAddition = noteAddition;
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
      projection.requireMatchingAcceptedTree(
          notebook, folders, liveNotes, proposal.repository(), acceptedHead);
      binding.clearAmendmentEligibility();
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
    Optional<NotebookGitProposalFolderCreationShape.RootFolderCreation> folderCreation =
        NotebookGitProposalFolderCreationShape.findSingleRootFolderCreation(files);
    if (folderCreation.isPresent()) {
      return folderAcceptance.acceptCreation(state, proposal, acceptedHead, folderCreation.get());
    }
    Optional<NotebookGitProposalFolderCreationShape.RootFolderAndContainedNoteCreation>
        folderAndContainedNote =
            NotebookGitProposalFolderCreationShape.findRootFolderAndContainedNoteCreation(files);
    if (folderAndContainedNote.isPresent()) {
      return folderAcceptance.acceptRootFolderAndContainedNote(
          state, proposal, acceptedHead, folderAndContainedNote.get());
    }
    Optional<NotebookGitProposalFolderCreationShape.InitialNotebookAndRootFolderCreation>
        initialCreation =
            NotebookGitProposalFolderCreationShape.findInitialNotebookAndRootFolderCreation(files);
    if (initialCreation.isPresent()) {
      return folderAcceptance.acceptInitialCreation(
          state, proposal, acceptedHead, initialCreation.get());
    }
    Optional<NotebookGitProposalFolderCreationShape.InitialNotebookRootFolderAndNoteCreation>
        initialNoteCreation =
            NotebookGitProposalFolderCreationShape.findInitialNotebookRootFolderAndNoteCreation(
                files);
    if (initialNoteCreation.isPresent()) {
      return folderAcceptance.acceptInitialCreationWithNote(
          state, proposal, acceptedHead, initialNoteCreation.get());
    }
    Optional<NotebookGitProposalInitialNotebookReadmePublication.CreationWithRootNote>
        initialNotebookReadmeAndNote =
            NotebookGitProposalInitialNotebookReadmePublication.findWithRootNote(files);
    if (initialNotebookReadmeAndNote.isPresent()) {
      return initialNotebookReadmePublication.acceptWithRootNote(
          state, proposal, acceptedHead, initialNotebookReadmeAndNote.get());
    }
    Optional<NotebookGitProposalInitialNotebookReadmePublication.Creation> initialNotebookReadme =
        NotebookGitProposalInitialNotebookReadmePublication.find(files);
    if (initialNotebookReadme.isPresent()) {
      return initialNotebookReadmePublication.accept(
          state, proposal, acceptedHead, initialNotebookReadme.get());
    }
    Optional<NotebookGitProposalInitialComposition.OneNoteInImpliedRootFolder>
        oneNoteInImpliedRootFolder =
            NotebookGitProposalInitialComposition.findOneNoteInImpliedRootFolder(files, proposal);
    if (oneNoteInImpliedRootFolder.isPresent() && folders.isEmpty() && liveNotes.isEmpty()) {
      return initialCompositionPublication.acceptOneNoteInImpliedRootFolder(
          state, proposal, acceptedHead, oneNoteInImpliedRootFolder.get());
    }
    Optional<NotebookGitProposalInitialComposition.ValidUnmatched> validUnmatchedInitial =
        NotebookGitProposalInitialComposition.findValidUnmatched(files, proposal);
    if (validUnmatchedInitial.isPresent()) {
      throw new IllegalStateException(
          "Initial Readme/Note composition is not a supported exact shape.");
    }
    Optional<NotebookGitProposalFolderShape.FolderRelocation> relocation =
        NotebookGitProposalFolderShape.requireExactOrEmpty(files);
    if (relocation.isPresent()) {
      return folderAcceptance.accept(state, proposal, acceptedHead, relocation.get());
    }
    List<NotebookGitProposalTreeShape.NoteChange> noteChanges =
        NotebookGitProposalTreeShape.requireAllowedNoteChangesFromInspectedFiles(files);
    NotebookGitProposalMarkdownFormat.assertValidTypedMarkdown(
        proposal.repository(), proposal.mainHead());
    projection.requireMatchingAcceptedTree(
        notebook, folders, liveNotes, proposal.repository(), acceptedHead);
    Timestamp publishedAt = testabilitySettings.getCurrentUTCTimestamp();
    List<Note> proposedLiveNotes = new ArrayList<>(liveNotes);
    for (NotebookGitProposalTreeShape.NoteChange noteChange : noteChanges) {
      if (noteChange.kind() == NotebookGitProposalTreeShape.ChangeKind.ADDED) {
        proposedLiveNotes.add(
            noteAddition.apply(
                notebook, folders, proposal, acceptedHead, noteChange.path(), publishedAt));
      } else if (noteChange.kind() == NotebookGitProposalTreeShape.ChangeKind.MODIFIED) {
        AuthoredNoteDocument document =
            noteAddition.readValidatedDocument(proposal, noteChange.path());
        Note changedNote =
            projection.requireOneLiveNoteAtPath(folders, liveNotes, noteChange.path());
        authoredNoteDocumentPersistence.persist(changedNote, document, publishedAt);
      } else if (noteChange.kind() == NotebookGitProposalTreeShape.ChangeKind.DELETED) {
        Note deletedNote =
            projection.requireOneLiveNoteAtPath(folders, liveNotes, noteChange.path());
        noteService.destroy(
            deletedNote,
            NoteDeleteReferenceHandling.LEAVE_DEAD_LINKS,
            authorizationService.getCurrentUser());
        proposedLiveNotes.remove(deletedNote);
      } else if (noteChange.kind() == NotebookGitProposalTreeShape.ChangeKind.RENAMED) {
        applyRename(notebook, folders, proposal, acceptedHead, liveNotes, noteChange, publishedAt);
      }
    }

    projection.requireMatchingAcceptedTree(
        notebook, folders, proposedLiveNotes, proposal.repository(), proposal.mainHead());

    return bindingPersistence.accept(binding, proposal, publishedAt);
  }

  private void applyRename(
      Notebook notebook,
      List<ExportFolderRow> folders,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      List<Note> liveNotes,
      NotebookGitProposalTreeShape.NoteChange noteChange,
      Timestamp publishedAt) {
    Note note = projection.requireOneLiveNoteAtPath(folders, liveNotes, noteChange.fromPath());
    String newTitle = filenameTitle.requireValid(noteChange.path());
    Folder destinationFolder =
        noteAddition.representedDestinationFolder(
            folders, proposal, acceptedHead, noteChange.path());
    try {
      noteTitlePlacementRules.requireNoSoftDeletedTitleAt(notebook, destinationFolder, newTitle);
    } catch (ApiException exception) {
      throw exception.withContext("Cannot rename to path \"" + noteChange.path() + "\"");
    }
    note.setTitle(new DisplayName(newTitle));
    note.setFolder(destinationFolder);
    note.setUpdatedAt(publishedAt);
    entityPersister.save(note);
  }
}
