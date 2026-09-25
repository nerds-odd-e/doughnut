package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Reserved Git metadata through publication and derived-tree preservation. */
class NotebookGitAttachmentMetadataControllerTest extends NotebookGitWebContentControllerTestBase {

  private static final String NOTE = "---\ntype: Note\n---\nbody\n";
  private static final String AUTHORED_ATTRIBUTES = "* filter=lfs -text\nauthored !filter\n";

  @Test
  void gitattributesAreReservedMetadataNotAttachmentsAndSurviveWebSaveAndReset() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    byte[] diagram = pointerFor(notebook, new byte[] {1, 2, 3});

    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(
            empty,
            List.of(
                new NotebookGitProposalFile("Note.md", NOTE),
                new NotebookGitProposalFile(".gitattributes", AUTHORED_ATTRIBUTES),
                new NotebookGitProposalFile("diagram.png", diagram))));

    assertThat(
        acceptedHistory(notebook).exactTree(),
        contains(
            PortableTreeEntry.ofText(".gitattributes", AUTHORED_ATTRIBUTES),
            PortableTreeEntry.ofText("Note.md", NOTE),
            new PortableTreeEntry("diagram.png", diagram)));
    assertThat(
        notebookAttachmentRepository.findByNotebook_Id(notebook.getId()).stream()
            .map(NotebookAttachment::getFilename)
            .toList(),
        contains("diagram.png"));

    textContentController.updateNoteContent(
        noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId()).getFirst(),
        contentDto("---\ntype: Note\n---\nedited\n"));

    assertThat(
        acceptedHistory(notebook).exactTree().stream()
            .filter(entry -> entry.path().equals(".gitattributes"))
            .map(entry -> new String(entry.content(), StandardCharsets.UTF_8))
            .toList(),
        contains(AUTHORED_ATTRIBUTES));
    assertAcceptedTreeMatchesTheFullAssembly(notebook);
    assertThat(
        acceptedHistory(notebook).exactTree().stream()
            .filter(entry -> entry.path().equals(".gitattributes"))
            .map(entry -> new String(entry.content(), StandardCharsets.UTF_8))
            .toList(),
        contains(AUTHORED_ATTRIBUTES));
  }
}
