package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookGit.NotebookGitAttributes;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.util.List;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;

/** Web note and folder edits keep LFS attributes, pointer identity, and learning. */
class NotebookGitAttachmentLfsWebContinuityControllerTest
    extends NotebookGitAttachmentLfsPublicationTestSupport {

  @Test
  void lfsWebEditsAndFolderOpsPreserveAttributesFileIdentityAndLearning() throws Exception {
    Notebook notebook = enableLfs(createGitBackedNotebook());
    NotebookGitBinding empty = snapshotCurrentPortableTree(notebook);
    byte[] payload = {(byte) 0xAB, (byte) 0xCD};
    byte[] pointer = pointerFor(notebook, payload);
    controller.publishNotebookGitProposal(
        notebook.getId(),
        empty.getAcceptedGitObjectId(),
        proposalBundleBytes(
            empty,
            List.of(
                new NotebookGitProposalFile(
                    NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT),
                new NotebookGitProposalFile("Root Note.md", NOTE_MARKDOWN),
                new NotebookGitProposalFile("Photos/alps.png", pointer))));
    Note note =
        noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId()).stream()
            .filter(n -> n.getTitle().equals("Root Note"))
            .findFirst()
            .orElseThrow();
    MemoryTracker tracker = learnedTracker(note, 3.0f, 1);
    ObjectId pointerBlobId = acceptedBlobIds(notebook).get("Photos/alps.png");
    Folder photos =
        folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
            .filter(folder -> folder.getName().equals("Photos"))
            .findFirst()
            .orElseThrow();

    textContentController.updateNoteContent(note, contentDto(EDITED_CONTENT));
    folderController.renameFolder(notebook, photos, renameTo("Pictures"));

    assertThat(
        acceptedHistory(notebook).tipContent(),
        containsInAnyOrder(
            PortableTreeEntry.ofText(
                NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT),
            new PortableTreeEntry("Pictures/alps.png", pointer),
            PortableTreeEntry.ofText("Root Note.md", EDITED_CONTENT)));
    assertThat(acceptedBlobIds(notebook).get("Pictures/alps.png"), equalTo(pointerBlobId));
    assertAcceptedTreeMatchesTheFullAssembly(notebook);
    assertShownContentAndRetainedLearning(note, tracker, EDITED_CONTENT);
  }
}
