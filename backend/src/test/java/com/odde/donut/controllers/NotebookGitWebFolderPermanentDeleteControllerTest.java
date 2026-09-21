package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Timestamp;
import java.time.Instant;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;

/**
 * Permanently deleting a trashed folder appends exactly one accepted commit whose tree lacks the
 * whole subtree.
 */
class NotebookGitWebFolderPermanentDeleteControllerTest
    extends NotebookGitWebContentControllerTestBase {
  private static final Instant DELETE_AT = Instant.parse("2026-09-18T11:00:00Z");
  private static final String CELLS_BODY = "---\ntype: Note\n---\ncells body";
  private static final String NUCLEUS_BODY = "---\ntype: Note\n---\nnucleus body";
  private static final String ATOMS_BODY = "---\ntype: Note\n---\natoms body";
  private static final byte[] PDF_BYTES = {(byte) 0x25, (byte) 0x50, (byte) 0x44, (byte) 0x46};

  @Test
  void permanentDeleteOfATrashedFolderAppendsOneAcceptedChildWithoutTheSubtree() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder topic =
        makeMe.aFolder().inTrashOf(notebook).name("Topic").readmeContent("Topic readme").please();
    Folder nested = makeMe.aFolder().parentFolder(topic).name("Nested").please();
    makeMe.aNote("Cells").folder(topic).content(CELLS_BODY).please();
    makeMe.aNote("Nucleus").folder(nested).content(NUCLEUS_BODY).please();
    Folder sibling =
        makeMe.aFolder().parentFolder(topic.getParentFolder()).name("Sibling").please();
    makeMe.aNote("Atoms").folder(sibling).content(ATOMS_BODY).please();
    Integer attachmentId =
        storeFolderAttachmentAndSnapshot(notebook, topic, "a.pdf", PDF_BYTES).getId();
    ObjectId acceptedA = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());
    testabilitySettings.timeTravelTo(Timestamp.from(DELETE_AT));

    folderController.permanentlyDeleteFolder(notebook, topic);

    ObjectId acceptedB = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());
    try (InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk revWalk = new RevWalk(repo)) {
      ObjectId downloadedHead =
          GitBundleTestReader.fetchHead(
              repo,
              controller
                  .downloadNotebookGitBundle(
                      notebookRepository.findById(notebook.getId()).orElseThrow())
                  .getBody());
      assertThat(downloadedHead, equalTo(acceptedB));
      RevCommit commitB = revWalk.parseCommit(downloadedHead);
      assertThat(commitB.getParentCount(), is(1));
      assertThat(commitB.getParent(0).getId(), equalTo(acceptedA));
      assertThat(
          GitBundleTestReader.pathsIn(repo, downloadedHead),
          containsInAnyOrder("_trash/Sibling/Atoms.md"));
      assertThat(
          GitBundleTestReader.readTreeEntries(repo, revWalk.parseCommit(acceptedA)),
          hasItem(new PortableTreeEntry("_trash/Topic/a.pdf", PDF_BYTES)));
    }
    assertThat(notebookAttachmentRepository.findById(attachmentId).isPresent(), is(false));
  }
}
