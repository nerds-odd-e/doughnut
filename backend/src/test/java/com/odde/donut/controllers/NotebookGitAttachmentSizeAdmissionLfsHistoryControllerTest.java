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
 * LFS attachment-size admission across unpublished first-parent history: within-limit intermediates
 * must be durable, new oversized intermediate-only payloads may be omitted when the tip is valid,
 * and previously accepted same-notebook digests are grandfathered.
 */
class NotebookGitAttachmentSizeAdmissionLfsHistoryControllerTest
    extends NotebookGitAttachmentSizeAdmissionLfsHistoryTestSupport {

  private static final long TWENTY_MIB = 20L * 1024 * 1024;
  private static final long THREE_MIB = 3L * 1024 * 1024;

  @Test
  void lfsAcceptsWithinLimitMultiVersionHistoryPreservingEarlierPayloadsAndCommitIds()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    byte[] first = filledBytes(256, (byte) 0x71);
    byte[] second = filledBytes(512, (byte) 0x72);
    byte[] firstPointer = pointerFor(notebook, first);
    byte[] secondPointer = pointerFor(notebook, second);
    ObjectId acceptedHead = ObjectId.fromString(empty.getAcceptedGitObjectId());
    LfsHistoryRange range =
        lfsReplaceThenKeepTipRange(
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
  void lfsUnchangedTipHistoryStillRequiresWithinLimitIntermediatePayload() throws Exception {
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
    LfsHistoryRange range =
        lfsIntroduceThenRemoveKeepingNote(
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
  void lfsAllowsOmittedOversizedIntermediateWhenTipCorrectionIsValid() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    byte[] over = filledBytes(TWENTY_MIB, (byte) 0x74);
    byte[] tipPayload = filledBytes(THREE_MIB, (byte) 0x75);
    byte[] overPointer = NotebookGitLfsPointer.format(sha256Hex(over), over.length);
    byte[] tipPointer = pointerFor(notebook, tipPayload);
    ObjectId acceptedHead = ObjectId.fromString(empty.getAcceptedGitObjectId());
    LfsHistoryRange range =
        lfsReplaceThenKeepTipRange(
            acceptedBundleBytes(notebook), acceptedHead, overPointer, tipPointer);

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), empty.getAcceptedGitObjectId(), range.proposalBytes());

    assertThat(publishedHead, equalTo(range.tip().getName()));
    assertThat(
        acceptedHistory(notebook).exactTree(),
        containsInAnyOrder(
            PortableTreeEntry.ofText(
                NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT),
            new PortableTreeEntry("version.bin", tipPointer)));
    assertThat(
        notebookAttachmentContent.get(notebook.getId(), sha256Hex(over)),
        equalTo(Optional.empty()));
    assertThat(
        notebookAttachmentContent.get(notebook.getId(), sha256Hex(tipPayload)).orElseThrow(),
        equalTo(tipPayload));
    assertPreservedFirstParentChain(notebook, acceptedHead, range.afterFirst(), range.tip());
  }

  @Test
  void lfsRefusesMissingWithinLimitIntermediateHistoryEvenWhenTipIsValid() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    byte[] missingIntermediate = filledBytes(320, (byte) 0x76);
    byte[] tipPayload = filledBytes(400, (byte) 0x77);
    byte[] missingPointer =
        NotebookGitLfsPointer.format(sha256Hex(missingIntermediate), missingIntermediate.length);
    byte[] tipPointer = pointerFor(notebook, tipPayload);
    ObjectId acceptedHead = ObjectId.fromString(empty.getAcceptedGitObjectId());
    LfsHistoryRange range =
        lfsReplaceThenKeepTipRange(
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

  @Test
  void lfsTrustedHistoryGrandfathersPreviouslyAcceptedOversizedPayload() throws Exception {
    byte[] oversized = filledBytes(LIMIT + 1, (byte) 0x78);
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote("Root Note").notebook(notebook).content(NOTE_MARKDOWN).please();
    byte[] pointer =
        storeFolderAttachmentAndSnapshot(notebook, null, "legacy.bin", oversized)
            .getAcceptedGitContent();
    NotebookGitBinding withLegacy = reloadCommittedBinding(notebook.getId());

    controller.publishNotebookGitProposal(
        notebook.getId(),
        withLegacy.getAcceptedGitObjectId(),
        proposalBundleBytes(
            withLegacy,
            List.of(
                new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN),
                new NotebookGitProposalFile("legacy.bin", pointer))));
    NotebookGitBinding kept = reloadCommittedBinding(notebook.getId());

    controller.publishNotebookGitProposal(
        notebook.getId(),
        kept.getAcceptedGitObjectId(),
        proposalBundleBytes(
            kept, List.of(new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN))));
    NotebookGitBinding withoutAttachment = reloadCommittedBinding(notebook.getId());

    controller.publishNotebookGitProposal(
        notebook.getId(),
        withoutAttachment.getAcceptedGitObjectId(),
        proposalBundleBytes(
            withoutAttachment,
            List.of(
                new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN),
                new NotebookGitProposalFile("restored.bin", pointer))));

    assertThat(
        acceptedHistory(notebook).exactTree(),
        containsInAnyOrder(
            PortableTreeEntry.ofText(
                NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT),
            PortableTreeEntry.ofText("Root Note.md", NOTE_MARKDOWN),
            new PortableTreeEntry("restored.bin", pointer)));
    assertThat(
        notebookAttachmentContent.get(notebook.getId(), sha256Hex(oversized)).orElseThrow(),
        equalTo(oversized));
  }
}
