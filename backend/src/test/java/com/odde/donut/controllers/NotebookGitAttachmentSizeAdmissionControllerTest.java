package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookGit.NotebookGitAttributes;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * The size limit applies to each attachment, and only this notebook's own accepted history
 * grandfathers an oversized payload.
 */
class NotebookGitAttachmentSizeAdmissionControllerTest
    extends NotebookGitAttachmentSizeAdmissionTestSupport {

  @Test
  void acceptsTwoWithinLimitAttachmentsWhoseCombinedSizeExceedsLimit() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    byte[] first = pointerFor(notebook, filledBytes(LIMIT / 2 + 1, (byte) 0x53));
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
  void oversizedPayloadIsGrandfatheredOnlyByThisNotebooksOwnAcceptedHistory() throws Exception {
    byte[] grandfathered = filledBytes(LIMIT + 1, (byte) 0x47);
    byte[] foreign = filledBytes(LIMIT + 1, (byte) 0x46);
    storeFolderAttachmentAndSnapshot(
        createGitBackedNotebook("Other Notebook"), null, "foreign.bin", foreign);
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote("Root Note").notebook(notebook).content(NOTE_MARKDOWN).please();
    byte[] legacyPointer =
        storeFolderAttachmentAndSnapshot(notebook, null, "legacy.bin", grandfathered)
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
}
