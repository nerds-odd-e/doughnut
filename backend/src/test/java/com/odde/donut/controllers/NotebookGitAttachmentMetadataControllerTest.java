package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.NotebookGitAttachmentRepresentation;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookGit.NotebookGitLfsPointer;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Reserved Git metadata and accepted Git attachment content through publication and derived-tree
 * preservation. Raw-subject cases use the legacy RAW fixture; the rest run on the product LFS
 * notebook.
 */
class NotebookGitAttachmentMetadataControllerTest extends NotebookGitWebContentControllerTestBase {

  private static final String NOTE = "---\ntype: Note\n---\nbody\n";
  private static final String POINTER_OID =
      "4d7a214614ab2935c943f9e0ff69d22eadbb8f32b1258daaa5e2ca24d17e2393";
  private static final String AUTHORED_ATTRIBUTES = "* filter=lfs -text\nauthored !filter\n";

  @Test
  void gitattributesAreReservedMetadataNotAttachmentsAndSurviveWebSaveAndReset() throws Exception {
    Notebook notebook = createLegacyRawNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);

    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(
            empty,
            List.of(
                new NotebookGitProposalFile("Note.md", NOTE),
                new NotebookGitProposalFile(".gitattributes", AUTHORED_ATTRIBUTES),
                new NotebookGitProposalFile("diagram.png", new byte[] {1, 2, 3}))));

    assertThat(
        acceptedHistory(notebook).tipContent(),
        contains(
            PortableTreeEntry.ofText(".gitattributes", AUTHORED_ATTRIBUTES),
            PortableTreeEntry.ofText("Note.md", NOTE),
            new PortableTreeEntry("diagram.png", new byte[] {1, 2, 3})));
    assertThat(
        notebookAttachmentRepository.findByNotebook_Id(notebook.getId()).stream()
            .map(NotebookAttachment::getFilename)
            .toList(),
        contains("diagram.png"));

    textContentController.updateNoteContent(
        noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId()).getFirst(),
        contentDto("---\ntype: Note\n---\nedited\n"));

    assertThat(
        acceptedHistory(notebook).tipContent().stream()
            .filter(entry -> entry.path().equals(".gitattributes"))
            .map(entry -> new String(entry.content(), StandardCharsets.UTF_8))
            .toList(),
        contains(AUTHORED_ATTRIBUTES));
    assertAcceptedTreeMatchesTheFullAssembly(notebook);
    assertThat(
        acceptedHistory(notebook).tipContent().stream()
            .filter(entry -> entry.path().equals(".gitattributes"))
            .map(entry -> new String(entry.content(), StandardCharsets.UTF_8))
            .toList(),
        contains(AUTHORED_ATTRIBUTES));
    assertThat(
        reloadCommittedBinding(notebook.getId()).getAttachmentRepresentation(),
        is(NotebookGitAttachmentRepresentation.RAW));
  }

  @Test
  void legacyPointerLookingPayloadsRemainLegacyBytesOnRawNotebooks() throws Exception {
    byte[] pointerLooking = NotebookGitLfsPointer.format(POINTER_OID, 7);
    Notebook notebook = createLegacyRawNotebook();
    NotebookAttachment attachment =
        storeAcceptedAttachmentAndSnapshot(notebook, null, "legacy.bin", pointerLooking);

    assertThat(attachment.getAcceptedGitContent(), equalTo(pointerLooking));
    assertThat(
        reloadCommittedBinding(notebook.getId()).getAttachmentRepresentation(),
        is(NotebookGitAttachmentRepresentation.RAW));
    assertThat(
        acceptedHistory(notebook).tipContent(),
        contains(new PortableTreeEntry("legacy.bin", pointerLooking)));
  }

  @Test
  void lfsRepresentationFixtureProjectsPointerBytesWithoutPublication() throws Exception {
    byte[] payload = "diagram".getBytes(StandardCharsets.UTF_8);
    Notebook notebook = createGitBackedNotebook();
    storeFolderAttachmentAndSnapshot(notebook, null, "diagram.png", payload);
    byte[] pointer = pointerFor(notebook, payload);

    assertThat(
        reloadCommittedBinding(notebook.getId()).getAttachmentRepresentation(),
        is(NotebookGitAttachmentRepresentation.LFS));
    assertThat(
        GitBundleTestReader.fetchAcceptedTip(acceptedBundleBytes(notebook)).content(),
        containsInAnyOrder(new PortableTreeEntry("diagram.png", pointer)));
    assertThat(
        notebookAttachmentRepository
            .findByNotebook_Id(notebook.getId())
            .getFirst()
            .getAcceptedGitContent(),
        equalTo(pointer));
  }
}
