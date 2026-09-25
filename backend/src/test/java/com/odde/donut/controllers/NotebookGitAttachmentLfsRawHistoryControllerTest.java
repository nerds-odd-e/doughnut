package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookGit.NotebookGitAttributes;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An LFS notebook whose accepted history still holds a raw file from before it moved to LFS keeps
 * that history readable and keeps accepting new pictures.
 */
class NotebookGitAttachmentLfsRawHistoryControllerTest
    extends NotebookGitWebContentControllerTestBase {

  private static final byte[] DIAGRAM = {(byte) 0x89, 'P', 'N', 'G', 1};
  private static final byte[] SKETCH = {(byte) 0x89, 'P', 'N', 'G', 2, (byte) 0xFF};

  @Autowired NotebookAttachmentController attachmentController;

  @Test
  void rawHistoryStaysReadableWhileANewPictureIsPublished() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder physics = makeMe.aFolder().notebook(notebook).name("physics").please();
    byte[] diagramPointer =
        storeFolderAttachmentAndSnapshot(notebook, physics, "diagram.png", DIAGRAM)
            .getAcceptedGitContent();
    ObjectId rawCommit;
    NotebookGitBinding accepted;
    try (InMemoryRepository history = new InMemoryRepository(new DfsRepositoryDescription())) {
      rawCommit =
          commitOnTopOf(
              history,
              List.of(),
              List.of(new NotebookGitProposalFile("physics/diagram.png", DIAGRAM)),
              "Raw picture");
      ObjectId lfsCommit =
          commitOnTopOf(
              history,
              List.of(rawCommit),
              List.of(
                  new NotebookGitProposalFile(
                      NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT),
                  new NotebookGitProposalFile("physics/diagram.png", diagramPointer)),
              "Store notebook files with Git LFS");
      accepted = seedAcceptedHistory(notebook, history, lfsCommit);
    }

    controller.publishNotebookGitProposal(
        notebook.getId(),
        accepted.getAcceptedGitObjectId(),
        proposalBundleBytes(
            accepted,
            List.of(
                new NotebookGitProposalFile("physics/diagram.png", diagramPointer),
                new NotebookGitProposalFile("physics/sketch.png", pointerFor(notebook, SKETCH)))));

    try (InMemoryRepository clone = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk revWalk = new RevWalk(clone)) {
      GitBundleTestReader.fetchHead(clone, acceptedBundleBytes(notebook));
      assertThat(
          GitBundleTestReader.readContent(clone, revWalk.parseCommit(rawCommit)),
          hasItem(new PortableTreeEntry("physics/diagram.png", DIAGRAM)));
    }
    Map<String, NotebookAttachment> rows = new HashMap<>();
    notebookAttachmentRepository
        .findByNotebook_Id(notebook.getId())
        .forEach(row -> rows.put(row.getFilename(), row));
    assertThat(download(notebook, rows.get("diagram.png")), equalTo(DIAGRAM));
    assertThat(download(notebook, rows.get("sketch.png")), equalTo(SKETCH));
  }

  private byte[] download(Notebook notebook, NotebookAttachment attachment) throws Exception {
    return attachmentController.downloadAttachment(notebook, attachment).getBody();
  }
}
