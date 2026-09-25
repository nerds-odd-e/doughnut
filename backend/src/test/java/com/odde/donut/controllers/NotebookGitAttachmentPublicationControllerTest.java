package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookGit.NotebookGitAttributes;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import com.odde.donut.testability.GitBundleTestReader.AcceptedTip;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;

class NotebookGitAttachmentPublicationControllerTest
    extends NotebookGitWebContentControllerTestBase {

  private static final String NOTE_MARKDOWN = "---\ntype: Note\n---\naccepted content";

  @Test
  void publishedRootFilesAreTheExactAcceptedTipAndSurviveTheNextWebNoteSave() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    ObjectId initialHead = ObjectId.fromString(empty.getAcceptedGitObjectId());
    // Names differing only in case are distinct files.
    byte[] capitalized = pointerFor(notebook, new byte[] {(byte) 0x80, 0x00, (byte) 0xC3});
    byte[] lowercase = pointerFor(notebook, new byte[] {(byte) 0x89, (byte) 0xFF, (byte) 0xFE});
    byte[] reference =
        pointerFor(notebook, "{\"schema\": \"donut\"}\n".getBytes(StandardCharsets.UTF_8));

    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(
            empty,
            List.of(
                new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN),
                new NotebookGitProposalFile("reference.json", reference),
                new NotebookGitProposalFile("Diagram.png", capitalized),
                new NotebookGitProposalFile("diagram.png", lowercase))));

    AcceptedTip published = acceptedTip(notebook);
    assertThat(published.ancestry().getFirst(), is(initialHead));
    assertThat(
        published.exactTree(),
        contains(
            PortableTreeEntry.ofText(
                NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT),
            new PortableTreeEntry("Diagram.png", capitalized),
            PortableTreeEntry.ofText("Root Note.md", NOTE_MARKDOWN),
            new PortableTreeEntry("diagram.png", lowercase),
            new PortableTreeEntry("reference.json", reference)));

    Note storedNote = noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId()).getFirst();
    textContentController.updateNoteContent(storedNote, contentDto(EDITED_CONTENT));

    AcceptedTip afterWebSave = acceptedTip(notebook);
    assertThat(afterWebSave.ancestry().getFirst(), is(published.head()));
    assertThat(
        afterWebSave.exactTree(),
        contains(
            PortableTreeEntry.ofText(
                NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT),
            new PortableTreeEntry("Diagram.png", capitalized),
            PortableTreeEntry.ofText("Root Note.md", EDITED_CONTENT),
            new PortableTreeEntry("diagram.png", lowercase),
            new PortableTreeEntry("reference.json", reference)));
  }

  private AcceptedTip acceptedTip(Notebook notebook) throws Exception {
    return GitBundleTestReader.fetchAcceptedTip(acceptedBundleBytes(notebook));
  }
}
