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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
      NoteTitlePlacementRules noteTitlePlacementRules,
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
    this.noteTitlePlacementRules = noteTitlePlacementRules;
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
    if (NotebookGitProposalTreeShape.isAdditionOnly(documents)) {
      projection.requireMatchingAcceptedTree(
          notebook, folders, liveNotes, proposal.repository(), acceptedHead);
      requireRepresentedDestinationsUnlessParentFolderIsAdded(
          folders, proposal, acceptedHead, documents, emptyAcceptedNotebook);
      return acceptMatchingProposedTree(
          documentApplication.apply(state, proposal, documents, publishedAt),
          proposal,
          publishedAt);
    }
    Optional<NotebookGitProposalFolderShape.FolderRelocation> relocation =
        NotebookGitProposalFolderShape.requireExactOrEmpty(files);
    if (relocation.isPresent()) {
      return acceptMatchingProposedTree(
          folderRelocation.apply(state, proposal, acceptedHead, relocation.get()), proposal);
    }
    List<NotebookGitProposalTreeShape.NoteChange> noteChanges =
        NotebookGitProposalTreeShape.requireAllowedNoteChanges(documents);
    NotebookGitProposalMarkdownFormat.assertValidTypedMarkdown(
        proposal.repository(), proposal.mainHead());
    projection.requireMatchingAcceptedTree(
        notebook, folders, liveNotes, proposal.repository(), acceptedHead);
    List<Note> proposedLiveNotes = new ArrayList<>(liveNotes);
    List<String> addedPaths = new ArrayList<>();
    for (NotebookGitProposalTreeShape.NoteChange noteChange : noteChanges) {
      if (noteChange.kind() == NotebookGitProposalTreeShape.ChangeKind.ADDED) {
        addedPaths.add(noteChange.path());
      }
    }
    List<NotebookGitProposalTreeShape.ChangedDocument> additions = new ArrayList<>();
    for (NotebookGitProposalTreeShape.ChangedDocument document : documents) {
      if (!addedPaths.contains(document.path())) {
        continue;
      }
      additions.add(document);
    }
    requireRepresentedDestinationsUnlessParentFolderIsAdded(
        folders, proposal, acceptedHead, additions, emptyAcceptedNotebook);
    for (NotebookGitProposalTreeShape.NoteChange noteChange : noteChanges) {
      if (noteChange.kind() == NotebookGitProposalTreeShape.ChangeKind.MODIFIED) {
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
    NotebookGitStateLoader.LockedNotebookState published =
        new NotebookGitStateLoader.LockedNotebookState(
            binding, notebook, folders, proposedLiveNotes);
    if (!additions.isEmpty()) {
      published = documentApplication.apply(published, proposal, additions, publishedAt);
    }
    return acceptMatchingProposedTree(published, proposal, publishedAt);
  }

  private static boolean isEmptyAcceptedNotebook(
      List<ExportFolderRow> folders,
      List<Note> liveNotes,
      List<NotebookGitProposalTreeShape.InspectedRegularFile> files) {
    return folders.isEmpty()
        && liveNotes.isEmpty()
        && files.stream().allMatch(file -> file.acceptedBlobId() == null);
  }

  private void requireRepresentedDestinationsUnlessParentFolderIsAdded(
      List<ExportFolderRow> folders,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      List<NotebookGitProposalTreeShape.ChangedDocument> documents,
      boolean emptyAcceptedNotebook) {
    if (emptyAcceptedNotebook) {
      return;
    }
    Set<String> addedContainerFolders = new HashSet<>();
    for (NotebookGitProposalTreeShape.ChangedDocument document : documents) {
      if (document.role() != NotebookGitProposalTreeShape.DocumentRole.CONTAINER) {
        continue;
      }
      String folderPath = parentFolderPath(document.path());
      if (folderPath != null) {
        addedContainerFolders.add(folderPath);
      }
    }
    for (NotebookGitProposalTreeShape.ChangedDocument document : documents) {
      if (document.role() != NotebookGitProposalTreeShape.DocumentRole.CONCEPT) {
        continue;
      }
      String folderPath = parentFolderPath(document.path());
      if (folderPath == null || addedContainerFolders.contains(folderPath)) {
        continue;
      }
      noteAddition.representedDestinationFolder(folders, proposal, acceptedHead, document.path());
    }
  }

  private static String parentFolderPath(String path) {
    int folderPathEnd = path.lastIndexOf('/');
    return folderPathEnd < 0 ? null : path.substring(0, folderPathEnd);
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
