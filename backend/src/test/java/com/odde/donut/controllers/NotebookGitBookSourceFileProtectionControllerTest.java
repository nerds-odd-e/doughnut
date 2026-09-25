package com.odde.donut.controllers;

import static com.odde.donut.controllers.NotebookBooksControllerTestBase.attachRequest;
import static com.odde.donut.controllers.NotebookBooksControllerTestBase.node;
import static com.odde.donut.controllers.NotebookBooksControllerTestBase.pdfFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import com.odde.donut.controllers.dto.AttachBookRequest;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** A local publish cannot delete, rename or change the file a Book reads from. */
class NotebookGitBookSourceFileProtectionControllerTest
    extends NotebookGitAttachmentSizeAdmissionTestSupport {
  @Autowired NotebookBooksController booksController;

  Notebook notebook;
  NotebookGitBinding accepted;
  byte[] bookPointer;

  @BeforeEach
  void attachPhysicsPrimer() throws Exception {
    notebook = createGitBackedNotebook();
    byte[] pdfBytes = {0x25, 0x50, 0x44, 0x46};
    AttachBookRequest request = attachRequest(node("Chapter 1"));
    request.setBookName("Physics Primer");
    booksController.attachBook(notebook, request, pdfFile(pdfBytes));
    accepted = reloadCommittedBinding(notebook.getId());
    bookPointer = lfsPointerStoredFor(notebook, pdfBytes);
  }

  @Test
  void deletingRenamingOrChangingTheBooksFileIsRefused() throws Exception {
    byte[] otherPointer = pointerFor(notebook, new byte[] {0x01, 0x02});
    List<List<NotebookGitProposalFile>> proposals =
        List.of(
            List.of(),
            List.of(new NotebookGitProposalFile("Renamed.pdf", bookPointer)),
            List.of(new NotebookGitProposalFile("Physics Primer.pdf", otherPointer)));

    for (List<NotebookGitProposalFile> files : proposals) {
      ResponseStatusException exception =
          assertProposalRejectedWithoutMutatingBinding(
              notebook,
              accepted.getAcceptedGitObjectId(),
              proposalBundleBytes(accepted, files),
              HttpStatus.CONFLICT);
      assertThat(
          exception.getReason(),
          equalTo(
              "\"Physics Primer.pdf\" is the source file of the Book \"Physics Primer\". Remove"
                  + " the Book on the web before deleting, renaming or changing it."));
    }
  }

  @Test
  void anUnrelatedChangeStillPublishes() throws Exception {
    byte[] otherPointer = pointerFor(notebook, new byte[] {0x01, 0x02});

    controller.publishNotebookGitProposal(
        notebook.getId(),
        accepted.getAcceptedGitObjectId(),
        proposalBundleBytes(
            accepted,
            List.of(
                new NotebookGitProposalFile("Physics Primer.pdf", bookPointer),
                new NotebookGitProposalFile("notes.bin", otherPointer))));

    assertThat(
        acceptedHistory(notebook).exactTree(),
        hasItem(new PortableTreeEntry("notes.bin", otherPointer)));
  }
}
