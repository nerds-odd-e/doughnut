package com.odde.donut.controllers;

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
import com.odde.donut.testability.GitBundleTestReader;
import com.odde.donut.testability.GitBundleTestReader.AcceptedTip;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** LFS tip publication admits valid pointers and refuses mixed invalid proposals. */
class NotebookGitAttachmentLfsPublicationControllerTest
    extends NotebookGitAttachmentLfsPublicationTestSupport {

  @Test
  void lfsPublicationAcceptsRootNestedAndEmptyPointersInGitAndProjection() throws Exception {
    Notebook notebook = enableLfs(createGitBackedNotebook());
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    byte[] rootPayload = {(byte) 0x89, 0x01, 0x02};
    byte[] nestedPayload = {(byte) 0xFF, 0x00};
    byte[] rootPointer = pointerFor(notebook, rootPayload);
    byte[] nestedPointer = pointerFor(notebook, nestedPayload);
    byte[] emptyFile = new byte[0];

    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(
            empty,
            List.of(
                new NotebookGitProposalFile(
                    NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT),
                new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN),
                new NotebookGitProposalFile("diagram.png", rootPointer),
                new NotebookGitProposalFile("tools/cache/nested.bin", nestedPointer),
                new NotebookGitProposalFile("empty.bin", emptyFile))));

    AcceptedTip published = acceptedTip(notebook);
    assertThat(
        published.entries(),
        containsInAnyOrder(
            PortableTreeEntry.ofText(
                NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT),
            new PortableTreeEntry("diagram.png", rootPointer),
            new PortableTreeEntry("empty.bin", emptyFile),
            PortableTreeEntry.ofText("Root Note.md", NOTE_MARKDOWN),
            new PortableTreeEntry("tools/cache/nested.bin", nestedPointer)));
    assertThat(
        NotebookLiveProjectionTestReader.attachmentTree(
            transactionManager, notebookAttachmentRepository, folderRepository, notebook.getId()),
        containsInAnyOrder(
            new PortableTreeEntry("diagram.png", rootPointer),
            new PortableTreeEntry("empty.bin", emptyFile),
            new PortableTreeEntry("tools/cache/nested.bin", nestedPointer)));
  }

  @Test
  void lfsExactLimitTipPointerIsAccepted() throws Exception {
    Notebook notebook = enableLfs(createGitBackedNotebook());
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    byte[] exact = filledBytes(LIMIT, (byte) 0x41);
    byte[] pointer = pointerFor(notebook, exact);

    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(
            empty,
            List.of(
                new NotebookGitProposalFile(
                    NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT),
                new NotebookGitProposalFile("payload.bin", pointer))));

    assertThat(
        acceptedTip(notebook).entries(),
        containsInAnyOrder(
            PortableTreeEntry.ofText(
                NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT),
            new PortableTreeEntry("payload.bin", pointer)));
    assertThat(
        NotebookLiveProjectionTestReader.attachmentTree(
                transactionManager,
                notebookAttachmentRepository,
                folderRepository,
                notebook.getId())
            .stream()
            .filter(entry -> entry.path().equals("payload.bin"))
            .map(PortableTreeEntry::content)
            .findFirst()
            .orElseThrow(),
        equalTo(pointer));
  }

  @Test
  void lfsMixedRefusalLeavesAcceptedHeadProjectionAndLearningUnchanged() throws Exception {
    Notebook notebook = enableLfs(createGitBackedNotebook());
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    byte[] acceptedPayload = {(byte) 0x10, 0x20};
    byte[] acceptedPointer = pointerFor(notebook, acceptedPayload);
    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(
            empty,
            List.of(
                new NotebookGitProposalFile(
                    NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT),
                new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN),
                new NotebookGitProposalFile("kept.png", acceptedPointer))));
    NotebookGitBinding accepted = reloadCommittedBinding(notebook.getId());
    Note note =
        noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId()).stream()
            .filter(n -> n.getTitle().equals("Root Note"))
            .findFirst()
            .orElseThrow();
    MemoryTracker tracker = learnedTracker(note, 4.5f, 2);
    List<PortableTreeEntry> tipBefore = acceptedTip(notebook).entries();
    List<PortableTreeEntry> projectionBefore =
        NotebookLiveProjectionTestReader.rootAttachments(
            transactionManager, notebookAttachmentRepository, notebook.getId());
    long objectRowsBefore = countNativeObjectStoreRows(accepted.getId());

    byte[] over = filledBytes(LIMIT + 1, (byte) 0x43);
    String overOid = sha256Hex(over);
    assertThat(
        notebookAttachmentContent.store(
            notebook.getId(), overOid, over.length, new ByteArrayInputStream(over)),
        is(true));
    byte[] overPointer = NotebookGitLfsPointer.format(overOid, over.length);
    byte[] missingPointer =
        NotebookGitLfsPointer.format(
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 3);
    byte[] corruptPointer = NotebookGitLfsPointer.format(sha256Hex(acceptedPayload), 99);

    ResponseStatusException overException =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            accepted.getAcceptedGitObjectId(),
            proposalBundleBytes(
                accepted,
                List.of(
                    new NotebookGitProposalFile(
                        NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT),
                    new NotebookGitProposalFile("Root Note.md", EDITED_CONTENT),
                    new NotebookGitProposalFile("kept.png", acceptedPointer),
                    new NotebookGitProposalFile("huge.bin", overPointer))),
            HttpStatus.BAD_REQUEST);
    assertThat(overException.getReason(), containsString("huge.bin"));
    assertThat(overException.getReason(), containsString(Long.toString(LIMIT + 1)));

    ResponseStatusException missingException =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            accepted.getAcceptedGitObjectId(),
            proposalBundleBytes(
                accepted,
                List.of(
                    new NotebookGitProposalFile(
                        NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT),
                    new NotebookGitProposalFile("Root Note.md", EDITED_CONTENT),
                    new NotebookGitProposalFile("kept.png", acceptedPointer),
                    new NotebookGitProposalFile("gone.bin", missingPointer))),
            HttpStatus.BAD_REQUEST);
    assertThat(missingException.getReason(), containsString("gone.bin"));
    assertThat(missingException.getReason(), containsString("missing"));

    ResponseStatusException corruptException =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            accepted.getAcceptedGitObjectId(),
            proposalBundleBytes(
                accepted,
                List.of(
                    new NotebookGitProposalFile(
                        NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT),
                    new NotebookGitProposalFile("Root Note.md", EDITED_CONTENT),
                    new NotebookGitProposalFile("kept.png", acceptedPointer),
                    new NotebookGitProposalFile("bad.bin", corruptPointer))),
            HttpStatus.BAD_REQUEST);
    assertThat(corruptException.getReason(), containsString("bad.bin"));
    assertThat(corruptException.getReason(), containsString("corrupt"));

    assertShownContentAndRetainedLearning(note, tracker, NOTE_MARKDOWN);
    assertThat(acceptedTip(notebook).entries(), equalTo(tipBefore));
    assertThat(
        NotebookLiveProjectionTestReader.rootAttachments(
            transactionManager, notebookAttachmentRepository, notebook.getId()),
        equalTo(projectionBefore));
    assertThat(countNativeObjectStoreRows(accepted.getId()), is(objectRowsBefore));
  }

  private AcceptedTip acceptedTip(Notebook notebook) throws Exception {
    return GitBundleTestReader.fetchAcceptedTip(acceptedBundleBytes(notebook));
  }
}
