package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.services.notebookGit.NotebookGitAttributes;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** A raw attachment payload from an older client is refused. */
class NotebookGitAttachmentCreationControllerTest
    extends NotebookGitAttachmentSizeAdmissionTestSupport {

  @Autowired NotebookAttachmentRepository notebookAttachmentRepository;

  @Test
  void olderClientRawAttachmentPayloadIntoLfsNotebookIsRefusedWithoutStoringBytes()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    byte[] rawPayload = {(byte) 0x89, (byte) 0xFF, (byte) 0xFE, 0x00};

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            empty.getAcceptedGitObjectId(),
            proposalBundleBytes(
                empty, List.of(new NotebookGitProposalFile("diagram.png", rawPayload))),
            HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("diagram.png"));
    assertThat(exception.getReason(), containsString("must be a Git LFS pointer"));
    assertThat(notebookAttachmentRepository.findByNotebook_Id(notebook.getId()), empty());
    assertThat(
        acceptedHistory(notebook).exactTree(),
        contains(
            PortableTreeEntry.ofText(
                NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT)));
  }
}
