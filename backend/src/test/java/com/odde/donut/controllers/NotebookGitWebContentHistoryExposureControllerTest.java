package com.odde.donut.controllers;

import static com.odde.donut.controllers.NotebookGitNoteCreationControllerTestSupport.titleOnly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.controllers.dto.NoteRealm;
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
import org.springframework.http.ResponseEntity;

class NotebookGitWebContentHistoryExposureControllerTest
    extends NotebookGitWebContentHistoryControllerTestSupport {

  @Test
  void downloadingBetweenQuickSavesDoesNotChangeAppendPolicy() throws Exception {
    Fixture fixture = fixture("Pulled");
    saveAt(fixture.noteId(), content("first"), T1000);
    ResponseEntity<byte[]> downloaded =
        controller.downloadNotebookGitBundle(
            notebookRepository.findById(fixture.notebookId()).orElseThrow());
    ObjectId downloadedHead;
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      downloadedHead = GitBundleTestReader.fetchHead(repository, downloaded.getBody());
    }

    saveAt(fixture.noteId(), content("second"), T1008);
    History afterSaves = historyFromBinding(fixture.notebookId(), "Pulled.md");
    assertThat(
        afterSaves.contentsNewestFirst(),
        equalTo(List.of(content("second"), content("first"), ACCEPTED_CONTENT)));
    assertThat(afterSaves.headsNewestFirst().get(1), equalTo(downloadedHead));
  }

  @Test
  void alternatingNotesYieldThreeCommitsAndCreationRemainsAncestor() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note noteA =
        makeMe.aNote().notebook(notebook).title("NoteA").content(ACCEPTED_CONTENT).please();
    Note noteB =
        makeMe.aNote().notebook(notebook).title("NoteB").content(ACCEPTED_CONTENT).please();
    snapshotCurrentPortableTree(notebook);

    NoteRealm created = controller.createNoteAtNotebookRoot(notebook, titleOnly("Created"));
    ObjectId creationHead = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());

    saveAt(noteA.getId(), content("A1"), T1000);
    saveAt(noteB.getId(), content("B1"), T1008);
    saveAt(noteA.getId(), content("A2"), T1016);

    History noteAHistory = historyFromBinding(notebook.getId(), "NoteA.md");
    assertThat(noteAHistory.contentsNewestFirst().size(), is(5));
    assertThat(noteAHistory.contentsNewestFirst().get(0), is(content("A2")));
    assertThat(noteAHistory.headsNewestFirst().contains(creationHead), is(true));

    ObjectId beforeCreatedEdit = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());
    saveAt(created.getId(), content("created-body"), T1027);
    byte[] downloadedAfterCreatedEdit = controller.downloadNotebookGitBundle(notebook).getBody();
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId tip = GitBundleTestReader.fetchHead(repository, downloadedAfterCreatedEdit);
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repository, tip, "Created.md"),
          is(content("created-body")));
      try (RevWalk revWalk = new RevWalk(repository)) {
        RevCommit commit = revWalk.parseCommit(tip);
        assertThat(commit.getParentCount(), is(1));
        assertThat(commit.getParent(0).getId(), equalTo(beforeCreatedEdit));
      }
    }
  }

  @Test
  void acceptedLocalPublicationBetweenSavesRemainsAncestorOfNextAppend() throws Exception {
    Fixture fixture = fixture("Local");
    saveAt(fixture.noteId(), content("web-1"), T1000);
    NotebookGitBinding afterWeb = bindingById(fixture.notebookId());
    ObjectId webHead = ObjectId.fromString(afterWeb.getAcceptedGitObjectId());
    byte[] proposal =
        proposalBundleBytes(
            afterWeb, List.of(new NotebookGitProposalFile("Local.md", content("local"))));
    ObjectId localHead;
    try (InMemoryRepository proposalRepo = new InMemoryRepository(new DfsRepositoryDescription())) {
      localHead = GitBundleTestReader.fetchHead(proposalRepo, proposal);
    }
    controller.publishNotebookGitProposal(
        fixture.notebookId(), afterWeb.getAcceptedGitObjectId(), proposal);

    saveAt(fixture.noteId(), content("web-2"), T1008);
    History history = historyFromBinding(fixture.notebookId(), "Local.md");
    assertThat(
        history.contentsNewestFirst(),
        equalTo(List.of(content("web-2"), content("local"), content("web-1"), ACCEPTED_CONTENT)));
    assertThat(history.headsNewestFirst().get(1), equalTo(localHead));
    assertThat(history.headsNewestFirst().get(2), equalTo(webHead));
  }

  @Test
  void queuedDownloadThenSaveThenFurtherSaveKeepsReturnedHeadsReachable() throws Exception {
    Fixture fixture = fixture("RaceDown");
    NotebookGitConcurrentWriterTestSupport.Result<ResponseEntity<byte[]>, NoteRealm> race =
        queuedWriters(
            fixture.notebookId(),
            () ->
                controller.downloadNotebookGitBundle(
                    notebookRepository.findById(fixture.notebookId()).orElseThrow()),
            () -> saveContent(fixture.noteId(), content("after-download")));
    ObjectId downloadedHead;
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      downloadedHead = GitBundleTestReader.fetchHead(repository, race.first().getBody());
    }
    saveAt(fixture.noteId(), content("further"), T1008);
    History history = historyFromBinding(fixture.notebookId(), "RaceDown.md");
    assertThat(history.headsNewestFirst().contains(downloadedHead), is(true));
  }

  @Test
  void queuedSaveThenDownloadThenFurtherSaveKeepsReturnedHeadsReachable() throws Exception {
    Fixture fixture = fixture("RaceSave");
    NotebookGitConcurrentWriterTestSupport.Result<NoteRealm, ResponseEntity<byte[]>> race =
        queuedWriters(
            fixture.notebookId(),
            () -> saveContent(fixture.noteId(), content("saved-first")),
            () ->
                controller.downloadNotebookGitBundle(
                    notebookRepository.findById(fixture.notebookId()).orElseThrow()));
    ObjectId downloadedHead;
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      downloadedHead = GitBundleTestReader.fetchHead(repository, race.second().getBody());
    }
    assertThat(
        downloadedHead.getName(),
        equalTo(bindingById(fixture.notebookId()).getAcceptedGitObjectId()));
    saveAt(fixture.noteId(), content("further"), T1008);
    History history = historyFromBinding(fixture.notebookId(), "RaceSave.md");
    assertThat(history.headsNewestFirst().contains(downloadedHead), is(true));
    assertThat(history.contentsNewestFirst().get(0), is(content("further")));
    assertThat(history.headsNewestFirst().get(1), equalTo(downloadedHead));
  }
}
