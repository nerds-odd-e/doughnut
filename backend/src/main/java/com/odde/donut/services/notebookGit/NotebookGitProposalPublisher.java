package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
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
import com.odde.donut.services.NoteFactory;
import com.odde.donut.services.NoteService;
import com.odde.donut.services.NoteTitlePlacementRules;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.testability.TestabilitySettings;
import com.odde.donut.validators.AuthoredNoteContent;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
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
  private final AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence;
  private final CanonicalDonutOrigin canonicalDonutOrigin;
  private final TestabilitySettings testabilitySettings;
  private final EntityPersister entityPersister;
  private final NotebookGitProposalFilenameTitle filenameTitle;
  private final NoteFactory noteFactory;
  private final NoteService noteService;
  private final NoteTitlePlacementRules noteTitlePlacementRules;

  public NotebookGitProposalPublisher(
      NotebookGitStateLoader notebookGitStateLoader,
      AuthorizationService authorizationService,
      NotebookGitProjection projection,
      AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence,
      CanonicalDonutOrigin canonicalDonutOrigin,
      TestabilitySettings testabilitySettings,
      EntityPersister entityPersister,
      NotebookGitProposalFilenameTitle filenameTitle,
      NoteFactory noteFactory,
      NoteService noteService,
      NoteTitlePlacementRules noteTitlePlacementRules) {
    this.notebookGitStateLoader = notebookGitStateLoader;
    this.authorizationService = authorizationService;
    this.projection = projection;
    this.authoredNoteDocumentPersistence = authoredNoteDocumentPersistence;
    this.canonicalDonutOrigin = canonicalDonutOrigin;
    this.testabilitySettings = testabilitySettings;
    this.entityPersister = entityPersister;
    this.filenameTitle = filenameTitle;
    this.noteFactory = noteFactory;
    this.noteService = noteService;
    this.noteTitlePlacementRules = noteTitlePlacementRules;
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

    List<NotebookGitProposalTreeShape.NoteChange> noteChanges =
        NotebookGitProposalTreeShape.requireRegularNoteChanges(
            proposal.repository(), acceptedHead, proposal.mainHead());
    NotebookGitProposalMarkdownFormat.assertValidTypedMarkdown(
        proposal.repository(), proposal.mainHead());
    projection.requireMatchingAcceptedTree(
        notebook, folders, liveNotes, proposal.repository(), acceptedHead);
    Timestamp publishedAt = testabilitySettings.getCurrentUTCTimestamp();
    List<Note> proposedLiveNotes = new ArrayList<>(liveNotes);
    for (NotebookGitProposalTreeShape.NoteChange noteChange : noteChanges) {
      if (noteChange.kind() == NotebookGitProposalTreeShape.ChangeKind.ADDED) {
        proposedLiveNotes.add(
            applyAddition(
                notebook, folders, proposal, acceptedHead, noteChange.path(), publishedAt));
      } else if (noteChange.kind() == NotebookGitProposalTreeShape.ChangeKind.MODIFIED) {
        AuthoredNoteDocument document = readValidatedDocument(proposal, noteChange.path());
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
        applyRename(folders, liveNotes, noteChange, publishedAt);
      }
    }

    projection.requireMatchingAcceptedTree(
        notebook, folders, proposedLiveNotes, proposal.repository(), proposal.mainHead());

    NotebookGitBundleWriter.BundleWriteResult written =
        NotebookGitBundleWriter.write(proposal.repository());
    binding.setAcceptedGitObjectId(written.headObjectId());
    binding.setBundleBytes(written.bundleBytes());
    binding.setUpdatedAt(publishedAt);
    entityPersister.save(binding);
    return written.headObjectId();
  }

  private Note applyAddition(
      Notebook notebook,
      List<ExportFolderRow> folders,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      String path,
      Timestamp publishedAt) {
    AuthoredNoteDocument document = readValidatedDocument(proposal, path);
    String title = filenameTitle.requireValid(path);
    Integer destinationFolderId =
        projection.requireRepresentedFolderIdForAddition(
            folders, proposal.repository(), acceptedHead, path);
    Folder destinationFolder =
        destinationFolderId == null
            ? null
            : entityPersister.find(Folder.class, destinationFolderId);
    Note addedNote;
    try {
      addedNote = noteFactory.create(notebook, destinationFolder, title);
    } catch (ApiException exception) {
      throw withContext(exception, "Cannot add note at path \"" + path + "\"");
    }
    authoredNoteDocumentPersistence.persist(addedNote, document, publishedAt);
    return addedNote;
  }

  private void applyRename(
      List<ExportFolderRow> folders,
      List<Note> liveNotes,
      NotebookGitProposalTreeShape.NoteChange noteChange,
      Timestamp publishedAt) {
    Note note = projection.requireOneLiveNoteAtPath(folders, liveNotes, noteChange.fromPath());
    String newTitle = filenameTitle.requireValid(noteChange.path());
    try {
      noteTitlePlacementRules.requireNoSoftDeletedTitleAt(
          note.getNotebook(), note.getFolder(), newTitle);
    } catch (ApiException exception) {
      throw withContext(exception, "Cannot rename to path \"" + noteChange.path() + "\"");
    }
    note.setTitle(new DisplayName(newTitle));
    note.setUpdatedAt(publishedAt);
    entityPersister.save(note);
  }

  private AuthoredNoteDocument readValidatedDocument(
      NotebookGitProposalImporter.ImportedProposal proposal, String path) {
    String content =
        NotebookGitProposalBlobText.readUtf8(proposal.repository(), proposal.mainHead(), path);
    try {
      AuthoredNoteContent.assertValidForSave(content);
    } catch (ApiException exception) {
      throw withContext(exception, "Invalid authored property at path \"" + path + "\"");
    }
    return AuthoredNoteDocument.fromContent(content, canonicalDonutOrigin);
  }

  private static ApiException withContext(ApiException exception, String context) {
    ApiException contextualException =
        new ApiException(
            exception.getMessage(),
            exception.getErrorBody().getErrorType(),
            context + ": " + exception.getErrorBody().getMessage());
    contextualException.getErrorBody().getErrors().putAll(exception.getErrorBody().getErrors());
    contextualException.initCause(exception);
    return contextualException;
  }
}
