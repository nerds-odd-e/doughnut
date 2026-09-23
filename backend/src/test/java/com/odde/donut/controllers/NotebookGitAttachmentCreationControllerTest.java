package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.NotebookGitAttachmentRepresentation;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import com.odde.donut.services.notebookGit.NotebookGitAttributes;
import com.odde.donut.services.notebookGit.NotebookGitLfsPointer;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Creation-time LFS activation and history-reset continuity through the real creation and cutover
 * entry points.
 */
class NotebookGitAttachmentCreationControllerTest
    extends NotebookGitAttachmentLfsPublicationTestSupport {

  @Autowired NotebookAttachmentRepository notebookAttachmentRepository;
  @Autowired NotebookAttachmentContent notebookAttachmentContent;

  @Test
  void productCreationSelectsLfsAndInstallsInitialAttributes() throws Exception {
    Notebook notebook = createProductLfsNotebook();
    NotebookGitBinding binding = reloadCommittedBinding(notebook.getId());

    assertThat(binding.getAttachmentRepresentation(), is(NotebookGitAttachmentRepresentation.LFS));
    assertThat(
        acceptedHistory(notebook).tipContent(),
        contains(
            PortableTreeEntry.ofText(
                NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT)));
  }

  @Test
  void demotedExistingNotebooksStayRawWithoutAttributesIncludingNeverCloned() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = reloadCommittedBinding(notebook.getId());

    assertThat(binding.getAttachmentRepresentation(), is(NotebookGitAttachmentRepresentation.RAW));
    assertThat(acceptedHistory(notebook).tipContent(), empty());
    assertThat(
        acceptedHistory(notebook).tipContent().stream().map(PortableTreeEntry::path).toList(),
        not(contains(NotebookGitAttributes.PATH)));
  }

  @Test
  void historyResetPreservesLfsRepresentationAttributesAndRetainedContentObjects()
      throws Exception {
    Notebook notebook = createProductLfsNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    byte[] payload = {(byte) 0x11, (byte) 0x22, (byte) 0x33};
    String oid = sha256Hex(payload);
    assertThat(
        notebookAttachmentContent.store(
            notebook.getId(), oid, payload.length, new ByteArrayInputStream(payload)),
        is(true));
    byte[] pointer = NotebookGitLfsPointer.format(oid, payload.length);
    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(
            empty,
            List.of(
                new NotebookGitProposalFile(
                    NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT),
                new NotebookGitProposalFile("payload.bin", pointer))));

    controller.resetNotebookGitHistory(notebookRepository.findById(notebook.getId()).orElseThrow());

    NotebookGitBinding afterReset = reloadCommittedBinding(notebook.getId());
    assertThat(
        afterReset.getAttachmentRepresentation(), is(NotebookGitAttachmentRepresentation.LFS));
    assertThat(
        acceptedHistory(notebook).tipContent(),
        contains(
            PortableTreeEntry.ofText(
                NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT),
            new PortableTreeEntry("payload.bin", pointer)));
    assertThat(
        notebookAttachmentContent.get(notebook.getId(), oid).orElseThrow(), equalTo(payload));
    assertThat(
        notebookAttachmentRepository.findByNotebook_Id(notebook.getId()).stream()
            .map(NotebookAttachment::getFilename)
            .toList(),
        contains("payload.bin"));
  }

  @Test
  void olderClientRawAttachmentPayloadIntoLfsNotebookIsRefusedWithoutStoringBytes()
      throws Exception {
    Notebook notebook = createProductLfsNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    byte[] rawPayload = {(byte) 0x89, (byte) 0xFF, (byte) 0xFE, 0x00};

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            empty.getAcceptedGitObjectId(),
            proposalBundleBytes(
                empty,
                List.of(
                    new NotebookGitProposalFile(
                        NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT),
                    new NotebookGitProposalFile("diagram.png", rawPayload))),
            HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("diagram.png"));
    assertThat(exception.getReason(), containsString("must be a Git LFS pointer"));
    assertThat(notebookAttachmentRepository.findByNotebook_Id(notebook.getId()), empty());
    assertThat(
        acceptedHistory(notebook).tipContent(),
        contains(
            PortableTreeEntry.ofText(
                NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT)));
  }
}
