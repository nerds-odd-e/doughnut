package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.controllers.dto.FolderMoveRequest;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.services.notebookTree.PortableTreeReadmeMarkdown;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Timestamp;
import java.time.Instant;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookGitWebFolderTrashControllerTest extends NotebookGitWebContentControllerTestBase {
  static final Instant TRASH_AT = Instant.parse("2026-09-16T12:00:00Z");
  static final Instant RECOVER_AT = Instant.parse("2026-09-16T12:05:00Z");
  static final String CELLS_BODY = "---\ntype: Note\n---\ncells body";
  static final String NUCLEUS_BODY = "---\ntype: Note\n---\nnucleus body";
  static final String BIOLOGY_README = "Biology readme";
  static final byte[] FORCE_DIAGRAM = {(byte) 0x89, (byte) 0xFF, (byte) 0xFE, 0x00};

  @Autowired FolderRepository folderRepository;

  @Test
  void trashOfFolderSubtreeAppendsAcceptedChildWithConstructedParentsReadmeDescendantsAndKeep()
      throws Exception {
    SubtreeFixture f = seedBiologySubtreeUnderResearch();
    ObjectId acceptedA = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    testabilitySettings.timeTravelTo(Timestamp.from(TRASH_AT));

    folderController.trashFolder(f.notebook(), f.biology());

    ObjectId acceptedB = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    try (InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloaded =
          GitBundleTestReader.fetchSingleParentCommit(repo, downloadedBundle(f.notebook()));
      assertThat(downloaded.head(), equalTo(acceptedB));
      assertThat(downloaded.parent(), equalTo(acceptedA));
      try (RevWalk revWalk = new RevWalk(repo)) {
        assertThat(revWalk.parseCommit(downloaded.head()).getParentCount(), is(1));
      }
      assertThat(
          GitBundleTestReader.pathsIn(repo, downloaded.head()),
          containsInAnyOrder(
              "Research/.keep",
              "_trash/Research/Biology/README.md",
              "_trash/Research/Biology/Cells.md",
              "_trash/Research/Biology/Nucleus.md",
              "_trash/Research/Biology/Empty/.keep"));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(
              repo, downloaded.head(), "_trash/Research/Biology/README.md"),
          equalTo(PortableTreeReadmeMarkdown.assemble(BIOLOGY_README)));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(
              repo, downloaded.head(), "_trash/Research/Biology/Cells.md"),
          equalTo(CELLS_BODY));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(
              repo, downloaded.head(), "_trash/Research/Biology/Nucleus.md"),
          equalTo(NUCLEUS_BODY));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(
              repo, downloaded.head(), "_trash/Research/Biology/Empty/.keep"),
          equalTo(""));
    }
    assertThat(parentFolderName(parentFolderId(f.biology().getId())), equalTo("Research"));
    assertThat(
        parentFolderName(parentFolderId(parentFolderId(f.biology().getId()))), equalTo("_trash"));
  }

  @Test
  void ordinaryMoveAfterActualFolderTrashAppendsAcceptedChildWithRecoveredPath() throws Exception {
    SubtreeFixture f = seedBiologySubtreeUnderResearch();
    testabilitySettings.timeTravelTo(Timestamp.from(TRASH_AT));
    folderController.trashFolder(f.notebook(), f.biology());
    ObjectId acceptedB = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    testabilitySettings.timeTravelTo(Timestamp.from(RECOVER_AT));

    folderController.moveFolder(f.notebook(), f.biology(), new FolderMoveRequest());

    ObjectId acceptedC = ObjectId.fromString(binding(f.notebook()).getAcceptedGitObjectId());
    try (InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloaded =
          GitBundleTestReader.fetchSingleParentCommit(repo, downloadedBundle(f.notebook()));
      assertThat(downloaded.head(), equalTo(acceptedC));
      assertThat(downloaded.parent(), equalTo(acceptedB));
      try (RevWalk revWalk = new RevWalk(repo)) {
        assertThat(revWalk.parseCommit(downloaded.head()).getParentCount(), is(1));
      }
      assertThat(
          GitBundleTestReader.pathsIn(repo, downloaded.head()),
          containsInAnyOrder(
              "Research/.keep",
              "Biology/README.md",
              "Biology/Cells.md",
              "Biology/Nucleus.md",
              "Biology/Empty/.keep",
              "_trash/Research/.keep"));
    }
    assertThat(parentFolderId(f.biology().getId()), nullValue());
  }

  @Test
  void webFolderTrashAndRecoveryCarryNestedAttachmentBytes() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder physics = makeMe.aFolder().notebook(notebook).name("physics").please();
    Folder diagrams = makeMe.aFolder().parentFolder(physics).name("diagrams").please();
    byte[] forcePointer =
        storeFolderAttachmentAndSnapshot(notebook, diagrams, "force.png", FORCE_DIAGRAM)
            .getAcceptedGitContent();

    folderController.trashFolder(notebook, physics);
    ObjectId trashedHead = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());
    folderController.moveFolder(notebook, physics, new FolderMoveRequest());

    try (InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk revWalk = new RevWalk(repo)) {
      ObjectId recoveredHead = GitBundleTestReader.fetchHead(repo, downloadedBundle(notebook));
      assertThat(
          GitBundleTestReader.readTreeEntries(repo, revWalk.parseCommit(trashedHead)),
          hasItem(new PortableTreeEntry("_trash/physics/diagrams/force.png", forcePointer)));
      assertThat(
          GitBundleTestReader.readTreeEntries(repo, revWalk.parseCommit(recoveredHead)),
          hasItem(new PortableTreeEntry("physics/diagrams/force.png", forcePointer)));
    }
  }

  @Test
  void nonGitNotebookFolderTrashCreatesNoBinding() throws UnexpectedNoAccessRightException {
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();

    folderController.trashFolder(notebook, biology);

    assertThat(parentFolderName(parentFolderId(biology.getId())), equalTo("_trash"));
    assertThat(
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).isPresent(), is(false));
  }

  SubtreeFixture seedBiologySubtreeUnderResearch() throws UnexpectedNoAccessRightException {
    Notebook notebook = createGitBackedNotebook();
    Folder research = makeMe.aFolder().notebook(notebook).name("Research").please();
    Folder biology =
        makeMe
            .aFolder()
            .parentFolder(research)
            .name("Biology")
            .readmeContent(BIOLOGY_README)
            .please();
    makeMe.aFolder().parentFolder(biology).name("Empty").please();
    makeMe.aNote("Cells").folder(biology).content(CELLS_BODY).please();
    makeMe.aNote("Nucleus").folder(biology).content(NUCLEUS_BODY).please();
    snapshotCurrentPortableTree(notebook);
    return new SubtreeFixture(notebook, research, biology);
  }

  byte[] downloadedBundle(Notebook notebook) throws UnexpectedNoAccessRightException {
    return controller
        .downloadNotebookGitBundle(notebookRepository.findById(notebook.getId()).orElseThrow())
        .getBody();
  }

  Integer parentFolderId(Integer folderId) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          Folder folder = folderRepository.findById(folderId).orElseThrow();
          return folder.getParentFolder() == null ? null : folder.getParentFolder().getId();
        });
  }

  String parentFolderName(Integer folderId) {
    return inCommittedTransaction(
        transactionManager, () -> folderRepository.findById(folderId).orElseThrow().getName());
  }

  record SubtreeFixture(Notebook notebook, Folder research, Folder biology) {}
}
