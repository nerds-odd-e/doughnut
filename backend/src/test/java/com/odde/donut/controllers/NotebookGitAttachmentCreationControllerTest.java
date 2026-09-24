package com.odde.donut.controllers;

import static com.odde.donut.services.notebookAttachment.VerifiedNotebookAttachmentBytes.sha256Hex;
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
import com.odde.donut.services.notebookGit.NotebookGitAttributes;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
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
    extends NotebookGitAttachmentSizeAdmissionTestSupport {

  @Autowired NotebookAttachmentRepository notebookAttachmentRepository;

  @Test
  void productCreationSelectsLfsAndInstallsInitialAttributes() throws Exception {
    Notebook notebook = createGitBackedNotebook();
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
    Notebook notebook = createLegacyRawNotebook();
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
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    byte[] payload = {(byte) 0x11, (byte) 0x22, (byte) 0x33};
    byte[] pointer = pointerFor(notebook, payload);
    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(empty, List.of(new NotebookGitProposalFile("payload.bin", pointer))));

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
        notebookAttachmentContent.get(notebook.getId(), sha256Hex(payload)).orElseThrow(),
        equalTo(payload));
    assertThat(
        notebookAttachmentRepository.findByNotebook_Id(notebook.getId()).stream()
            .map(NotebookAttachment::getFilename)
            .toList(),
        contains("payload.bin"));
  }

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
        acceptedHistory(notebook).tipContent(),
        contains(
            PortableTreeEntry.ofText(
                NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT)));
  }
}
