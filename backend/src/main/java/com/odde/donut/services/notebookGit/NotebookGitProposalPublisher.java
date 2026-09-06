package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.AuthoredNoteDocumentPersistence;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.testability.TestabilitySettings;
import com.odde.donut.validators.AuthoredNoteContent;
import java.sql.Timestamp;
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

  public NotebookGitProposalPublisher(
      NotebookGitStateLoader notebookGitStateLoader,
      AuthorizationService authorizationService,
      NotebookGitProjection projection,
      AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence,
      CanonicalDonutOrigin canonicalDonutOrigin,
      TestabilitySettings testabilitySettings,
      EntityPersister entityPersister) {
    this.notebookGitStateLoader = notebookGitStateLoader;
    this.authorizationService = authorizationService;
    this.projection = projection;
    this.authoredNoteDocumentPersistence = authoredNoteDocumentPersistence;
    this.canonicalDonutOrigin = canonicalDonutOrigin;
    this.testabilitySettings = testabilitySettings;
    this.entityPersister = entityPersister;
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

    String changedNotePath =
        NotebookGitProposalTreeShape.assertSingleModifiedRegularNotePath(
            proposal.repository(), acceptedHead, proposal.mainHead());
    NotebookGitProposalMarkdownFormat.assertValidTypedMarkdown(
        proposal.repository(), proposal.mainHead());
    String changedNoteContent =
        NotebookGitProposalBlobText.readUtf8(
            proposal.repository(), proposal.mainHead(), changedNotePath);
    AuthoredNoteContent.assertValidForSave(changedNoteContent);
    Note changedNote =
        projection.requireMatchingAcceptedTreeWithOneLiveNoteAtPath(
            notebook, folders, liveNotes, proposal.repository(), acceptedHead, changedNotePath);

    Timestamp publishedAt = testabilitySettings.getCurrentUTCTimestamp();
    AuthoredNoteDocument document =
        AuthoredNoteDocument.fromContent(changedNoteContent, canonicalDonutOrigin);
    authoredNoteDocumentPersistence.persist(changedNote, document, publishedAt);
    projection.requireMatchingAcceptedTree(
        notebook, folders, liveNotes, proposal.repository(), proposal.mainHead());

    NotebookGitBundleWriter.BundleWriteResult written =
        NotebookGitBundleWriter.write(proposal.repository());
    binding.setAcceptedGitObjectId(written.headObjectId());
    binding.setBundleBytes(written.bundleBytes());
    binding.setUpdatedAt(publishedAt);
    entityPersister.save(binding);
    return written.headObjectId();
  }
}
