package com.odde.donut.controllers;

import static com.odde.donut.services.notebookAttachment.VerifiedNotebookAttachmentBytes.sha256Hex;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookGit.NotebookGitAttributes;
import com.odde.donut.services.notebookGit.NotebookGitLfsPointer;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Attachment admission: the inclusive size limit applies to each attachment, only an oversized
 * payload this notebook's accepted head already holds is admitted, and every refusal leaves the
 * notebook unchanged.
 */
class NotebookGitAttachmentSizeAdmissionControllerTest
    extends NotebookGitAttachmentSizeAdmissionTestSupport {

  @Test
  void limitIsInclusiveAndAppliesToEachAttachmentNotTheirCombinedSize() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    byte[] first = pointerFor(notebook, filledBytes(LIMIT, (byte) 0x53));
    byte[] second = pointerFor(notebook, filledBytes(LIMIT / 2 + 1, (byte) 0x54));

    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(
            empty,
            List.of(
                new NotebookGitProposalFile("a.bin", first),
                new NotebookGitProposalFile("b.bin", second))));

    assertThat(
        acceptedHistory(notebook).exactTree(),
        containsInAnyOrder(
            PortableTreeEntry.ofText(
                NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT),
            new PortableTreeEntry("a.bin", first),
            new PortableTreeEntry("b.bin", second)));
  }

  @Test
  void oversizedPayloadIsAdmittedOnlyWhenThisNotebooksAcceptedHeadHoldsIt() throws Exception {
    byte[] legacy = filledBytes(LIMIT + 1, (byte) 0x47);
    byte[] foreign = filledBytes(LIMIT + 1, (byte) 0x46);
    storeFolderAttachmentAndSnapshot(
        createGitBackedNotebook("Other Notebook"), null, "foreign.bin", foreign);
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote("Root Note").notebook(notebook).content(NOTE_MARKDOWN).please();
    byte[] legacyPointer =
        storeFolderAttachmentAndSnapshot(notebook, null, "legacy.bin", legacy)
            .getAcceptedGitContent();
    NotebookGitBinding accepted = reloadCommittedBinding(notebook.getId());

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            accepted.getAcceptedGitObjectId(),
            proposalBundleBytes(
                accepted,
                List.of(
                    new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN),
                    new NotebookGitProposalFile("legacy.bin", legacyPointer),
                    new NotebookGitProposalFile("foreign.bin", pointerFor(notebook, foreign)))),
            HttpStatus.BAD_REQUEST);

    assertOversizedRefusal(exception, "foreign.bin", LIMIT + 1);
  }

  @Test
  void refusedAttachmentsLeaveAcceptedHeadProjectionContentAndLearningUnchanged() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    byte[] acceptedPayload = {(byte) 0x10, 0x20};
    byte[] acceptedPointer = pointerFor(notebook, acceptedPayload);
    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(
            empty,
            List.of(
                new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN),
                new NotebookGitProposalFile("kept.png", acceptedPointer))));
    NotebookGitBinding accepted = reloadCommittedBinding(notebook.getId());
    Note note =
        noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId()).stream()
            .filter(n -> n.getTitle().equals("Root Note"))
            .findFirst()
            .orElseThrow();
    MemoryTracker tracker = learnedTracker(note, 4.5f, 2);
    List<PortableTreeEntry> tipBefore = acceptedHistory(notebook).exactTree();
    List<PortableTreeEntry> projectionBefore = committedRootAttachments(notebook);
    long objectRowsBefore = countNativeObjectStoreRows(accepted.getId());

    byte[] over = filledBytes(LIMIT + 1, (byte) 0x43);
    String overOid = sha256Hex(over);
    assertThat(
        notebookAttachmentContent.store(
            notebook.getId(), overOid, over.length, new ByteArrayInputStream(over)),
        is(true));
    record Refusal(String path, byte[] content, String reason) {}
    List<Refusal> refusals =
        List.of(
            new Refusal(
                "huge.bin",
                NotebookGitLfsPointer.format(overOid, over.length),
                Long.toString(LIMIT + 1)),
            new Refusal("gone.bin", NotebookGitLfsPointer.format("a".repeat(64), 3), "missing"),
            new Refusal(
                "bad.bin", NotebookGitLfsPointer.format(sha256Hex(acceptedPayload), 99), "corrupt"),
            new Refusal(
                "diagram.png",
                new byte[] {(byte) 0x89, (byte) 0xFF, (byte) 0xFE, 0x00},
                "must be a Git LFS pointer or empty file"));

    for (Refusal refusal : refusals) {
      ResponseStatusException exception =
          assertProposalRejectedWithoutMutatingBinding(
              notebook,
              accepted.getAcceptedGitObjectId(),
              proposalBundleBytes(
                  accepted,
                  List.of(
                      new NotebookGitProposalFile("Root Note.md", EDITED_CONTENT),
                      new NotebookGitProposalFile("kept.png", acceptedPointer),
                      new NotebookGitProposalFile(refusal.path(), refusal.content()))),
              HttpStatus.BAD_REQUEST);
      assertThat(exception.getReason(), containsString("\"" + refusal.path() + "\""));
      assertThat(exception.getReason(), containsString(refusal.reason()));
    }

    assertShownContentAndRetainedLearning(note, tracker, NOTE_MARKDOWN);
    assertThat(acceptedHistory(notebook).exactTree(), equalTo(tipBefore));
    assertThat(committedRootAttachments(notebook), equalTo(projectionBefore));
    assertThat(countNativeObjectStoreRows(accepted.getId()), is(objectRowsBefore));
  }
}
