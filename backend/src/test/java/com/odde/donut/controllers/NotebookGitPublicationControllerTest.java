package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.NoteRecallInfo;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/** Verifies the accepted publication round trip across Note projection and Git history. */
class NotebookGitPublicationControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String ORIGINAL_CONTENT = "---\ntype: Note\n---\nOriginal authored bytes.\n";
  private static final String TARGET_CONTENT = "---\ntype: Note\n---\nReference target.\n";
  private static final String PUBLISHED_CONTENT =
      "---\n"
          + "type: FieldObservation\n"
          + "confidence: 7\n"
          + "reviewed: false\n"
          + "---\n"
          + "Precisely preserved authored bytes linking [[Reference Target|the target]].\n";

  @Autowired NoteController noteController;

  @Test
  void publishesExactCommitOnTheSameLearnedNoteAndMakesItDownloadable() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder folder = makeMe.aFolder().notebook(notebook).name("Research").please();
    Note target =
        makeMe
            .aNote()
            .notebook(notebook)
            .title("Reference Target")
            .content(TARGET_CONTENT)
            .please();
    Note refinedNote =
        makeMe.aNote().folder(folder).title("Refined Note").content(ORIGINAL_CONTENT).please();
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(refinedNote.getId()).orElseThrow())
                    .difficulty(7f)
                    .please());
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Reference Target.md", TARGET_CONTENT),
                new NotebookGitProposalFile("Research/Refined Note.md", PUBLISHED_CONTENT)));

    ObjectId proposedHead;
    ObjectId proposedTree;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedHead = GitBundleTestReader.fetchHead(proposal, proposalBytes);
      try (RevWalk revWalk = new RevWalk(proposal)) {
        RevCommit proposedCommit = revWalk.parseCommit(proposedHead);
        proposedTree = proposedCommit.getTree().getId();
        assertThat(proposedCommit.getParent(0).getId(), equalTo(acceptedHead));
      }
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Note reloadedNote = noteRepository.findById(refinedNote.getId()).orElseThrow();
    NoteRealm noteRealm = noteController.showNote(reloadedNote);
    NoteRecallInfo recallInfo = noteController.getNoteInfo(reloadedNote);
    assertThat(publishedHead, equalTo(proposedHead.getName()));
    assertThat(noteRealm.getId(), equalTo(refinedNote.getId()));
    assertThat(noteRealm.getNote().getContent(), equalTo(PUBLISHED_CONTENT));
    assertThat(noteRealm.getWikiLinks(), hasSize(1));
    assertThat(
        noteRealm.getWikiLinks().getFirst().getAuthoredLink(),
        equalTo("Reference Target|the target"));
    assertThat(
        noteRealm.getWikiLinks().getFirst().getResolution(), equalTo(WikiLink.Resolution.RESOLVED));
    assertThat(noteRealm.getWikiLinks().getFirst().getDestinationNoteId(), equalTo(target.getId()));
    assertThat(recallInfo.getMemoryTrackers(), hasSize(1));
    assertThat(recallInfo.getMemoryTrackers().getFirst().getId(), equalTo(tracker.getId()));
    assertThat(recallInfo.getMemoryTrackers().getFirst().getDifficulty(), equalTo(7f));

    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId downloadedHead = GitBundleTestReader.fetchHead(readBack, downloaded.getBody());
      assertThat(downloadedHead, equalTo(proposedHead));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(
              readBack, downloadedHead, "Research/Refined Note.md"),
          equalTo(PUBLISHED_CONTENT));
      try (RevWalk revWalk = new RevWalk(readBack)) {
        RevCommit downloadedCommit = revWalk.parseCommit(downloadedHead);
        assertThat(downloadedCommit.getTree().getId(), equalTo(proposedTree));
        assertThat(downloadedCommit.getParent(0).getId(), equalTo(acceptedHead));
        assertThat(revWalk.parseCommit(acceptedHead).getId(), equalTo(acceptedHead));
      }
    }
  }
}
