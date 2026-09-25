package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.services.notebookGit.NotebookGitAttributes;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import com.odde.donut.testability.GitBundleTestReader.AcceptedTip;
import java.util.List;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookGitAttachmentPublicationControllerTest
    extends NotebookGitWebContentControllerTestBase {

  @Autowired FolderRepository folderRepository;

  @Test
  void publishedFilesAreTheExactAcceptedTipAndProjectionAndSurviveTheNextWebNoteSave()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    ObjectId initialHead = ObjectId.fromString(empty.getAcceptedGitObjectId());
    // Names differing only in case are distinct files.
    byte[] capitalized = pointerFor(notebook, new byte[] {(byte) 0x80, 0x00, (byte) 0xC3});
    byte[] lowercase = pointerFor(notebook, new byte[] {(byte) 0x89, (byte) 0xFF, (byte) 0xFE});
    byte[] nested = pointerFor(notebook, new byte[] {(byte) 0xFF, 0x00});
    byte[] emptyFile = new byte[0];

    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(
            empty,
            List.of(
                new NotebookGitProposalFile("Root Note.md", ACCEPTED_CONTENT),
                new NotebookGitProposalFile("Diagram.png", capitalized),
                new NotebookGitProposalFile("diagram.png", lowercase),
                new NotebookGitProposalFile("tools/cache/nested.bin", nested),
                new NotebookGitProposalFile("empty.bin", emptyFile))));

    AcceptedTip published = acceptedTip(notebook);
    assertThat(published.ancestry().getFirst(), is(initialHead));
    assertThat(
        published.exactTree(),
        contains(
            PortableTreeEntry.ofText(
                NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT),
            new PortableTreeEntry("Diagram.png", capitalized),
            PortableTreeEntry.ofText("Root Note.md", ACCEPTED_CONTENT),
            new PortableTreeEntry("diagram.png", lowercase),
            new PortableTreeEntry("empty.bin", emptyFile),
            new PortableTreeEntry("tools/cache/nested.bin", nested)));
    assertThat(
        NotebookLiveProjectionTestReader.attachmentTree(
            transactionManager, notebookAttachmentRepository, folderRepository, notebook.getId()),
        containsInAnyOrder(
            new PortableTreeEntry("Diagram.png", capitalized),
            new PortableTreeEntry("diagram.png", lowercase),
            new PortableTreeEntry("empty.bin", emptyFile),
            new PortableTreeEntry("tools/cache/nested.bin", nested)));

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
            new PortableTreeEntry("empty.bin", emptyFile),
            new PortableTreeEntry("tools/cache/nested.bin", nested)));
  }

  private AcceptedTip acceptedTip(Notebook notebook) throws Exception {
    return GitBundleTestReader.fetchAcceptedTip(acceptedBundleBytes(notebook));
  }
}
