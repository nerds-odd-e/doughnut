package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.entities.repositories.NotebookRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.services.notebookExport.NotebookExportRows;
import com.odde.donut.validators.AuthoredNoteContent;
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

  private final NotebookGitBindingRepository bindingRepository;
  private final NotebookRepository notebookRepository;
  private final FolderRepository folderRepository;
  private final NoteRepository noteRepository;
  private final AuthorizationService authorizationService;
  private final NotebookGitProjection projection;

  public NotebookGitProposalPublisher(
      NotebookGitBindingRepository bindingRepository,
      NotebookRepository notebookRepository,
      FolderRepository folderRepository,
      NoteRepository noteRepository,
      AuthorizationService authorizationService,
      NotebookGitProjection projection) {
    this.bindingRepository = bindingRepository;
    this.notebookRepository = notebookRepository;
    this.folderRepository = folderRepository;
    this.noteRepository = noteRepository;
    this.authorizationService = authorizationService;
    this.projection = projection;
  }

  @Transactional(
      propagation = Propagation.REQUIRES_NEW,
      isolation = Isolation.SERIALIZABLE,
      rollbackFor = Exception.class)
  public void validateForPublish(
      Integer notebookId,
      String expectedHead,
      NotebookGitProposalImporter.ImportedProposal proposal)
      throws UnexpectedNoAccessRightException {
    NotebookGitBinding binding =
        bindingRepository
            .findByNotebookIdForUpdate(notebookId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Notebook has no Git binding."));
    Notebook notebook =
        notebookRepository
            .findById(notebookId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notebook not found."));
    List<ExportFolderRow> folders = NotebookExportRows.folders(folderRepository, notebook);
    List<Note> liveNotes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebookId);

    authorizationService.assertAuthorization(notebook);
    if (!expectedHead.equals(binding.getAcceptedGitObjectId())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "expectedHead no longer matches the notebook's current accepted head.");
    }
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    NotebookGitProposalAncestry.assertFollowsAcceptedHead(
        proposal.repository(), proposal.mainHead(), acceptedHead);
    if (proposal.mainHead().equals(acceptedHead)) {
      projection.requireMatchingAcceptedTree(
          notebook, folders, liveNotes, proposal.repository(), acceptedHead);
      return;
    }

    String changedNotePath =
        NotebookGitProposalTreeShape.assertSingleModifiedRegularNotePath(
            proposal.repository(), acceptedHead, proposal.mainHead());
    NotebookGitProposalMarkdownFormat.assertValidTypedMarkdown(
        proposal.repository(), proposal.mainHead());
    String changedNoteContent =
        NotebookGitProposalBlobText.readUtf8(
            proposal.repository(), proposal.mainHead(), changedNotePath);
    AuthoredNoteContent.assertValidForSave(changedNoteContent);
    projection.requireMatchingAcceptedTreeWithOneLiveNoteAtPath(
        notebook, folders, liveNotes, proposal.repository(), acceptedHead, changedNotePath);
  }
}
