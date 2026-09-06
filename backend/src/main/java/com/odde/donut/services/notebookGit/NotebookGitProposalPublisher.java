package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.AuthoredNoteDocumentPersistence;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.NoteFactory;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.testability.TestabilitySettings;
import com.odde.donut.validators.AuthoredNoteContent;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
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
  private final AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence;
  private final CanonicalDonutOrigin canonicalDonutOrigin;
  private final TestabilitySettings testabilitySettings;
  private final EntityPersister entityPersister;
  private final Validator validator;
  private final NoteFactory noteFactory;

  public NotebookGitProposalPublisher(
      NotebookGitStateLoader notebookGitStateLoader,
      AuthorizationService authorizationService,
      NotebookGitProjection projection,
      AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence,
      CanonicalDonutOrigin canonicalDonutOrigin,
      TestabilitySettings testabilitySettings,
      EntityPersister entityPersister,
      Validator validator,
      NoteFactory noteFactory) {
    this.notebookGitStateLoader = notebookGitStateLoader;
    this.authorizationService = authorizationService;
    this.projection = projection;
    this.authoredNoteDocumentPersistence = authoredNoteDocumentPersistence;
    this.canonicalDonutOrigin = canonicalDonutOrigin;
    this.testabilitySettings = testabilitySettings;
    this.entityPersister = entityPersister;
    this.validator = validator;
    this.noteFactory = noteFactory;
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

    NotebookGitProposalTreeShape.NoteChange noteChange =
        NotebookGitProposalTreeShape.requireSingleRegularNoteChange(
            proposal.repository(), acceptedHead, proposal.mainHead());
    NotebookGitProposalMarkdownFormat.assertValidTypedMarkdown(
        proposal.repository(), proposal.mainHead());
    Timestamp publishedAt = testabilitySettings.getCurrentUTCTimestamp();
    List<Note> proposedLiveNotes = liveNotes;
    if (noteChange.kind() == NotebookGitProposalTreeShape.ChangeKind.ADDED) {
      projection.requireMatchingAcceptedTree(
          notebook, folders, liveNotes, proposal.repository(), acceptedHead);
      Note addedNote =
          applyAddition(notebook, folders, proposal, acceptedHead, noteChange.path(), publishedAt);
      proposedLiveNotes = new ArrayList<>(liveNotes);
      proposedLiveNotes.add(addedNote);
    } else {
      AuthoredNoteDocument document = readValidatedDocument(proposal, noteChange.path());
      Note changedNote =
          projection.requireMatchingAcceptedTreeWithOneLiveNoteAtPath(
              notebook, folders, liveNotes, proposal.repository(), acceptedHead, noteChange.path());
      authoredNoteDocumentPersistence.persist(changedNote, document, publishedAt);
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
    String title = validAdditionTitle(path);
    Integer destinationFolderId =
        projection.requireRepresentedFolderIdForAddition(
            folders, proposal.repository(), acceptedHead, path);
    Folder destinationFolder =
        destinationFolderId == null
            ? null
            : entityPersister.find(Folder.class, destinationFolderId);
    Note addedNote = noteFactory.create(notebook, destinationFolder, title);
    authoredNoteDocumentPersistence.persist(addedNote, document, publishedAt);
    return addedNote;
  }

  private AuthoredNoteDocument readValidatedDocument(
      NotebookGitProposalImporter.ImportedProposal proposal, String path) {
    String content =
        NotebookGitProposalBlobText.readUtf8(proposal.repository(), proposal.mainHead(), path);
    AuthoredNoteContent.assertValidForSave(content);
    return AuthoredNoteDocument.fromContent(content, canonicalDonutOrigin);
  }

  private String validAdditionTitle(String path) {
    String filename = path.substring(path.lastIndexOf('/') + 1);
    String title = filename.substring(0, filename.length() - ".md".length());
    NoteUpdateTitleDTO titleDto = new NoteUpdateTitleDTO();
    titleDto.setNewTitle(title);
    Set<ConstraintViolation<NoteUpdateTitleDTO>> violations = validator.validate(titleDto);
    if (!violations.isEmpty()) {
      String reason =
          violations.stream()
              .map(ConstraintViolation::getMessage)
              .sorted()
              .findFirst()
              .orElseThrow();
      throw invalidAdditionTitle(path, reason);
    }
    String normalizedTitle = new DisplayName(title).value();
    if (!normalizedTitle.equals(title)) {
      throw invalidAdditionTitle(
          path, "filename title would be normalized to \"" + normalizedTitle + "\"");
    }
    return title;
  }

  private static ResponseStatusException invalidAdditionTitle(String path, String reason) {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Invalid note title at path \"" + path + "\": " + reason);
  }
}
