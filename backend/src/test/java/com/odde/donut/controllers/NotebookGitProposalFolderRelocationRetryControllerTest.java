package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.services.notebookExport.ExportReadmeMarkdown;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Verifies retrying an accepted exact folder relocation does not reapply it. */
class NotebookGitProposalFolderRelocationRetryControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String README_BODY = "readme";
  private static final String README = ExportReadmeMarkdown.assemble(README_BODY);
  private static final String NOTE = "---\ntype: Note\n---\nnote";

  @Autowired FolderRepository folderRepository;

  @Test
  void retriesAnAcceptedFolderRelocationWithoutChangingParentIdsOrBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder topics =
        makeMe.aFolder().notebook(notebook).name("Topics").readmeContent(README_BODY).please();
    Folder archive =
        makeMe.aFolder().notebook(notebook).name("Archive").readmeContent(README_BODY).please();
    makeMe.aNote().folder(topics).title("A").content(NOTE).please();
    NotebookGitBinding initialBinding = snapshotCurrentPortableTree(notebook);
    String initialHead = initialBinding.getAcceptedGitObjectId();
    byte[] proposalBytes =
        proposalBundleBytes(
            initialBinding,
            List.of(
                new NotebookGitProposalFile("Archive/README.md", README),
                new NotebookGitProposalFile("Archive/Topics/README.md", README),
                new NotebookGitProposalFile("Archive/Topics/A.md", NOTE)));

    String publishedHead =
        controller.publishNotebookGitProposal(notebook.getId(), initialHead, proposalBytes);
    PublicationState stateAfterPublication = publicationState(notebook, topics.getId());
    var acceptedAfterPublication = acceptedHistory(notebook);
    assertThat(stateAfterPublication.sourceParentId(), equalTo(archive.getId()));

    String retriedHead =
        controller.publishNotebookGitProposal(notebook.getId(), initialHead, proposalBytes);

    assertThat(retriedHead, equalTo(publishedHead));
    PublicationState stateAfterRetry = publicationState(notebook, topics.getId());
    assertThat(stateAfterRetry.acceptedHead(), equalTo(stateAfterPublication.acceptedHead()));
    assertThat(
        stateAfterRetry.bindingUpdatedAt(), equalTo(stateAfterPublication.bindingUpdatedAt()));
    assertThat(acceptedHistory(notebook), equalTo(acceptedAfterPublication));
    assertThat(stateAfterRetry.sourceParentId(), equalTo(stateAfterPublication.sourceParentId()));
    assertThat(stateAfterRetry.folderIds(), equalTo(stateAfterPublication.folderIds()));
    assertThat(stateAfterRetry.noteIds(), equalTo(stateAfterPublication.noteIds()));
  }

  private PublicationState publicationState(Notebook notebook, Integer sourceFolderId) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          NotebookGitBinding binding =
              notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
          Folder source = folderRepository.findById(sourceFolderId).orElseThrow();
          return new PublicationState(
              binding.getAcceptedGitObjectId(),
              binding.getUpdatedAt(),
              source.getParentFolderId(),
              folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
                  .map(Folder::getId)
                  .toList(),
              noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId()).stream()
                  .map(Note::getId)
                  .toList());
        });
  }

  private record PublicationState(
      String acceptedHead,
      Timestamp bindingUpdatedAt,
      Integer sourceParentId,
      List<Integer> folderIds,
      List<Integer> noteIds) {}
}
