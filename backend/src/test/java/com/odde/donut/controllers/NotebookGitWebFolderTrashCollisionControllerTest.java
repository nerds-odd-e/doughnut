package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Timestamp;
import java.time.Instant;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;

class NotebookGitWebFolderTrashCollisionControllerTest
    extends NotebookGitWebContentControllerTestBase {
  static final Instant TRASH_AT = Instant.parse("2026-09-16T12:00:00Z");
  static final String EARLIER_BODY = "---\ntype: Note\n---\nearlier cells";
  static final String LATER_BODY = "---\ntype: Note\n---\nlater cells";
  static final String INCOMING_BODY = "---\ntype: Note\n---\nincoming cells";

  @Test
  void occupiedTrashNameSuffixingLeavesEarlierTrashBytesUnchanged() throws Exception {
    CollisionFixture f = seedIncomingBiologyWithOccupiedTrashNames();
    testabilitySettings.timeTravelTo(Timestamp.from(TRASH_AT));

    Folder result = folderController.trashFolder(f.notebook(), f.incoming());

    assertThat(result.getName(), equalTo("Biology (4)"));
    try (InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId downloadedHead =
          GitBundleTestReader.fetchHead(
              repo,
              controller
                  .downloadNotebookGitBundle(
                      notebookRepository.findById(f.notebook().getId()).orElseThrow())
                  .getBody());
      assertThat(
          GitBundleTestReader.pathsIn(repo, downloadedHead),
          containsInAnyOrder(
              "_trash/Biology/Earlier.md",
              "_trash/biology (2)/.keep",
              "_trash/Biology (3)/Later.md",
              "_trash/Biology (4)/Incoming.md"));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "_trash/Biology/Earlier.md"),
          equalTo(EARLIER_BODY));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "_trash/Biology (3)/Later.md"),
          equalTo(LATER_BODY));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(
              repo, downloadedHead, "_trash/Biology (4)/Incoming.md"),
          equalTo(INCOMING_BODY));
    }
  }

  CollisionFixture seedIncomingBiologyWithOccupiedTrashNames()
      throws UnexpectedNoAccessRightException {
    Notebook notebook = createGitBackedNotebook();
    Folder incoming = makeMe.aFolder().notebook(notebook).name("Biology").please();
    makeMe.aNote("Incoming").folder(incoming).content(INCOMING_BODY).please();
    Folder trash = makeMe.aFolder().notebook(notebook).name("_trash").please();
    Folder earlier = makeMe.aFolder().parentFolder(trash).name("Biology").please();
    makeMe.aNote("Earlier").folder(earlier).content(EARLIER_BODY).please();
    makeMe.aFolder().parentFolder(trash).name("biology (2)").please();
    Folder later = makeMe.aFolder().parentFolder(trash).name("Biology (3)").please();
    makeMe.aNote("Later").folder(later).content(LATER_BODY).please();
    snapshotCurrentPortableTree(notebook);
    return new CollisionFixture(notebook, incoming);
  }

  record CollisionFixture(Notebook notebook, Folder incoming) {}
}
