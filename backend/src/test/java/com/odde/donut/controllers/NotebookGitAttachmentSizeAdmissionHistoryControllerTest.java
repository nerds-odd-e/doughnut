package com.odde.donut.controllers;

import static com.odde.donut.services.notebookAttachment.VerifiedNotebookAttachmentBytes.sha256Hex;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookGit.NotebookGitAttributes;
import com.odde.donut.services.notebookGit.NotebookGitLfsPointer;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.util.List;
import java.util.Optional;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Attachment admission across unpublished first-parent history: every attachment an intermediate
 * commit changes must be durable, not only the tip's.
 */
class NotebookGitAttachmentSizeAdmissionHistoryControllerTest
    extends NotebookGitAttachmentSizeAdmissionHistoryTestSupport {

  @Test
  void acceptsWithinLimitMultiVersionHistoryPreservingEarlierPayloadsAndCommitIds()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    byte[] first = filledBytes(256, (byte) 0x71);
    byte[] second = filledBytes(512, (byte) 0x72);
    byte[] firstPointer = pointerFor(notebook, first);
    byte[] secondPointer = pointerFor(notebook, second);
    ObjectId acceptedHead = ObjectId.fromString(empty.getAcceptedGitObjectId());
    HistoryRange range =
        replaceThenKeepTipRange(
            acceptedBundleBytes(notebook), acceptedHead, firstPointer, secondPointer);

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), empty.getAcceptedGitObjectId(), range.proposalBytes());

    assertThat(publishedHead, equalTo(range.tip().getName()));
    assertThat(
        acceptedHistory(notebook).exactTree(),
        containsInAnyOrder(
            PortableTreeEntry.ofText(
                NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT),
            new PortableTreeEntry("version.bin", secondPointer)));
    assertThat(
        notebookAttachmentContent.get(notebook.getId(), sha256Hex(first)).orElseThrow(),
        equalTo(first));
    assertThat(
        notebookAttachmentContent.get(notebook.getId(), sha256Hex(second)).orElseThrow(),
        equalTo(second));
    assertPreservedFirstParentChain(notebook, acceptedHead, range.afterFirst(), range.tip());
  }

  @Test
  void unchangedTipHistoryStillRequiresWithinLimitIntermediatePayload() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(
            empty, List.of(new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN))));
    NotebookGitBinding accepted = reloadCommittedBinding(notebook.getId());
    ObjectId acceptedHead = ObjectId.fromString(accepted.getAcceptedGitObjectId());
    byte[] temporary = filledBytes(384, (byte) 0x73);
    byte[] temporaryPointer = pointerFor(notebook, temporary);
    HistoryRange range =
        introduceThenRemoveKeepingNote(
            acceptedBundleBytes(notebook), acceptedHead, temporaryPointer);

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), accepted.getAcceptedGitObjectId(), range.proposalBytes());

    assertThat(publishedHead, equalTo(range.tip().getName()));
    assertThat(
        notebookAttachmentContent.get(notebook.getId(), sha256Hex(temporary)).orElseThrow(),
        equalTo(temporary));
  }

  @Test
  void refusesMissingWithinLimitIntermediateHistoryEvenWhenTipIsValid() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    byte[] missingIntermediate = filledBytes(320, (byte) 0x76);
    byte[] tipPayload = filledBytes(400, (byte) 0x77);
    byte[] missingPointer =
        NotebookGitLfsPointer.format(sha256Hex(missingIntermediate), missingIntermediate.length);
    byte[] tipPointer = pointerFor(notebook, tipPayload);
    ObjectId acceptedHead = ObjectId.fromString(empty.getAcceptedGitObjectId());
    HistoryRange range =
        replaceThenKeepTipRange(
            acceptedBundleBytes(notebook), acceptedHead, missingPointer, tipPointer);

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            empty.getAcceptedGitObjectId(),
            range.proposalBytes(),
            HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString("version.bin"));
    assertThat(exception.getReason(), containsString("missing"));
    assertThat(
        notebookAttachmentContent.get(notebook.getId(), sha256Hex(missingIntermediate)),
        equalTo(Optional.empty()));
  }
}
