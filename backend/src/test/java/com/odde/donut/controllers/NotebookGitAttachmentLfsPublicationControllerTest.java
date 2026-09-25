package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.services.notebookGit.NotebookGitAttributes;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import com.odde.donut.testability.GitBundleTestReader.AcceptedTip;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** LFS tip publication admits valid pointers. */
class NotebookGitAttachmentLfsPublicationControllerTest
    extends NotebookGitAttachmentSizeAdmissionTestSupport {

  @Autowired FolderRepository folderRepository;

  @Test
  void lfsPublicationAcceptsRootNestedAndEmptyPointersInGitAndProjection() throws Exception {
    Notebook notebook = createGitBackedNotebook();
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
                new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN),
                new NotebookGitProposalFile("diagram.png", rootPointer),
                new NotebookGitProposalFile("tools/cache/nested.bin", nestedPointer),
                new NotebookGitProposalFile("empty.bin", emptyFile))));

    AcceptedTip published = acceptedTip(notebook);
    assertThat(
        published.exactTree(),
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

  private AcceptedTip acceptedTip(Notebook notebook) throws Exception {
    return GitBundleTestReader.fetchAcceptedTip(acceptedBundleBytes(notebook));
  }
}
