package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.AuthoredNoteDocumentPersistence;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.NoteService;
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
  private final NotebookGitProposalFolderRelocation folderRelocation;
  private final NotebookGitProposalDocumentApplication documentApplication;
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
      NotebookGitProposalFolderRelocation folderRelocation,
      NotebookGitProposalDocumentApplication documentApplication,
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
    this.folderRelocation = folderRelocation;
    this.documentApplication = documentApplication;
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
    if (documents.isEmpty()) {
      projection.requireMatchingAcceptedTree(
          notebook, folders, liveNotes, proposal.repository(), acceptedHead);
      return acceptMatchingProposedTree(state, proposal, publishedAt);
    }
    Optional<NotebookGitProposalFolderShape.FolderRelocation> relocation =
        NotebookGitProposalFolderShape.requireExactOrCarried(
            proposal.repository(), acceptedHead, proposal.mainHead(), files);
    NotebookGitStateLoader.LockedNotebookState published;
    final NotebookGitProposalTreeShape.AdmittedShape admitted;
    if (relocation.isPresent()) {
      documents =
          NotebookGitProposalTreeShape.classifyChangedDocuments(
              NotebookGitProposalFolderShape.residualOutside(files, relocation.get()));
      if (documents.isEmpty()) {
        published = folderRelocation.apply(state, proposal, acceptedHead, relocation.get());
        return acceptMatchingProposedTree(published, proposal);
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
      NotebookGitProposalMarkdownFormat.assertValidTypedMarkdown(
          proposal.repository(), proposal.mainHead());
      projection.requireMatchingAcceptedTree(
          notebook, folders, liveNotes, proposal.repository(), acceptedHead);
      published =
          new NotebookGitStateLoader.LockedNotebookState(binding, notebook, folders, liveNotes);
      if (!admitted.additions().isEmpty()) {
        published =
            documentApplication.apply(published, proposal, admitted.additions(), publishedAt);
      }
    }
    List<Note> proposedLiveNotes = new ArrayList<>(published.liveNotes());
    List<ExportFolderRow> proposedFolders = published.folders();
    for (NotebookGitProposalTreeShape.NoteChange noteChange : admitted.noteChanges()) {
      if (noteChange.kind() == NotebookGitProposalTreeShape.ChangeKind.MODIFIED) {
        AuthoredNoteDocument document =
            noteAddition.readValidatedDocument(proposal, noteChange.path());
        Note changedNote =
            projection.requireOneLiveNoteAtPath(
                proposedFolders, proposedLiveNotes, noteChange.path());
        authoredNoteDocumentPersistence.persist(changedNote, document, publishedAt);
      } else if (noteChange.kind() == NotebookGitProposalTreeShape.ChangeKind.DELETED) {
        Note deletedNote =
            projection.requireOneLiveNoteAtPath(
                proposedFolders, proposedLiveNotes, noteChange.path());
        noteService.permanentlyRemove(
            deletedNote,
            NoteDeleteReferenceHandling.LEAVE_DEAD_LINKS,
            authorizationService.getCurrentUser());
        proposedLiveNotes.remove(deletedNote);
      } else if (noteChange.kind() == NotebookGitProposalTreeShape.ChangeKind.RENAMED) {
        applyRename(
            proposedFolders, proposal, acceptedHead, proposedLiveNotes, noteChange, publishedAt);
      }
    }
    return acceptMatchingProposedTree(
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

  private String acceptMatchingProposedTree(
      NotebookGitStateLoader.LockedNotebookState published,
      NotebookGitProposalImporter.ImportedProposal proposal) {
    return acceptMatchingProposedTree(
        published, proposal, testabilitySettings.getCurrentUTCTimestamp());
  }

  private String acceptMatchingProposedTree(
      NotebookGitStateLoader.LockedNotebookState published,
      NotebookGitProposalImporter.ImportedProposal proposal,
      Timestamp publishedAt) {
    projection.requireMatchingAcceptedTree(
        published.notebook(),
        published.folders(),
        published.liveNotes(),
        proposal.repository(),
        proposal.mainHead());
    return bindingPersistence.accept(published.binding(), proposal, publishedAt);
  }

  private void applyRename(
      List<ExportFolderRow> folders,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      List<Note> liveNotes,
      NotebookGitProposalTreeShape.NoteChange noteChange,
      Timestamp publishedAt) {
    Note note = projection.requireOneLiveNoteAtPath(folders, liveNotes, noteChange.origin().path());
    String newTitle = filenameTitle.requireValid(noteChange.path());
    Folder destinationFolder =
        noteAddition.representedDestinationFolder(
            folders, proposal, acceptedHead, noteChange.path());
    note.setTitle(new DisplayName(newTitle));
    note.setFolder(destinationFolder);
    note.setUpdatedAt(publishedAt);
    entityPersister.save(note);
    if (!noteChange.blobId().equals(noteChange.origin().blobId())) {
      AuthoredNoteDocument document =
          noteAddition.readValidatedDocument(proposal, noteChange.path());
      authoredNoteDocumentPersistence.persist(note, document, publishedAt);
    }
  }
}
