package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookGit.NotebookGitCutoverService;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NotebookGitWebContentAmendmentControllerTest
    extends NotebookGitWebContentAmendmentControllerTestSupport {

  private static final String NOTE_PATH = "Root Note.md";
  private static final String CONTENT_1000 = "---\ntype: Note\n---\ncontent at 10:00";
  private static final String CONTENT_1008 = "---\ntype: Note\n---\ncontent at 10:08";
  private static final String CONTENT_1016 = "---\ntype: Note\n---\ncontent at 10:16";

  @Test
  void continuousSameNoteSavesAt1000Then1008Then1016KeepOneEditCommitWithFinalContent()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("Root Note").content(ACCEPTED_CONTENT).please();
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(note.getId()).orElseThrow())
                    .difficulty(7f)
                    .please());
    NotebookGitBinding accepted = snapshotCurrentPortableTree(notebook);
    ObjectId preEditParent = ObjectId.fromString(accepted.getAcceptedGitObjectId());

    saveAt(note.getId(), CONTENT_1000, T1000);
    saveAt(note.getId(), CONTENT_1008, T1008);
    saveAt(note.getId(), CONTENT_1016, T1016);

    Note reloaded = noteRepository.findById(note.getId()).orElseThrow();
    assertThat(reloaded.getContent(), is(CONTENT_1016));
    assertThat(reloaded.getUpdatedAt(), is(Timestamp.from(T1016)));
    assertThat(
        noteController.getNoteInfo(reloaded).getMemoryTrackers().getFirst().getId(),
        is(tracker.getId()));
    assertThat(
        noteController.getNoteInfo(reloaded).getMemoryTrackers().getFirst().getDifficulty(),
        is(7f));

    History history = downloadHistory(notebook, NOTE_PATH);
    assertThat(history.contentsNewestFirst(), equalTo(List.of(CONTENT_1016, ACCEPTED_CONTENT)));
    assertThat(history.headsNewestFirst().get(1), equalTo(preEditParent));
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId tip = GitBundleTestReader.fetchHead(repository, history.bundleBytes());
      try (RevWalk revWalk = new RevWalk(repository)) {
        RevCommit commit = revWalk.parseCommit(tip);
        assertThat(commit.getParentCount(), is(1));
        assertThat(commit.getParent(0).getId(), equalTo(preEditParent));
        assertThat(
            commit.getAuthorIdent().getName(), is(NotebookGitCutoverService.SYSTEM_AUTHOR_NAME));
        assertThat(
            commit.getAuthorIdent().getEmailAddress(),
            is(NotebookGitCutoverService.SYSTEM_AUTHOR_EMAIL));
        assertThat(commit.getAuthorIdent().getWhenAsInstant(), equalTo(T1000));
        assertThat(commit.getCommitterIdent().getWhenAsInstant(), equalTo(T1016));
        assertThat(commit.getFullMessage(), is("Edit note content: Root Note"));
      }
    }
  }

  @Nested
  class EligibilityBoundary {
    @Test
    void fractionalSaveWithinBatchAmendsAndPersistsMillisecondClock() throws Exception {
      Fixture fixture = fixture("FracWithin");
      saveAt(fixture.noteId(), content("first"), T1000_600);
      NotebookGitBinding afterFirst =
          inCommittedTransaction(
              transactionManager,
              () ->
                  notebookGitBindingRepository
                      .findByNotebook_Id(fixture.notebookId())
                      .orElseThrow());
      assertThat(afterFirst.getAmendmentLastChangedAt().toInstant(), equalTo(T1000_600));
      saveAt(fixture.noteId(), content("soon"), T1000_800);
      assertThat(contentChain(fixture.notebookId(), "FracWithin.md").size(), is(2));
    }

    @Test
    void fractionalSaveJustBelowTenMinutesStillAmends() throws Exception {
      Fixture fixture = fixture("FracBelow");
      saveAt(fixture.noteId(), content("first"), T1000_600);
      saveAt(fixture.noteId(), content("below"), T1010_599);
      assertThat(contentChain(fixture.notebookId(), "FracBelow.md").size(), is(2));
    }

    @Test
    void fractionalSaveAtExactTenMinutesAppendsNewEditCommit() throws Exception {
      Fixture fixture = fixture("FracExact");
      saveAt(fixture.noteId(), content("first"), T1000_600);
      saveAt(fixture.noteId(), content("exact"), T1010_600);
      assertThat(contentChain(fixture.notebookId(), "FracExact.md").size(), is(3));
    }

    @Test
    void justBelowTenMinutesAmendsWhileExactAndLongerAppend() throws Exception {
      Fixture fixture = fixture("Boundary");
      saveAt(fixture.noteId(), content("first"), T1000);
      saveAt(fixture.noteId(), content("below"), T100959);
      assertThat(contentChain(fixture.notebookId(), "Boundary.md").size(), is(2));

      Instant exactTenAfterLast = Instant.parse("2026-09-08T10:19:59Z");
      saveAt(fixture.noteId(), content("exact"), exactTenAfterLast);
      assertThat(contentChain(fixture.notebookId(), "Boundary.md").size(), is(3));

      saveAt(fixture.noteId(), content("later"), Instant.parse("2026-09-08T10:30:00Z"));
      assertThat(contentChain(fixture.notebookId(), "Boundary.md").size(), is(4));
    }

    @Test
    void noOpBetweenChangedSavesDoesNotExtendEligibilityOrChangeGitBytes() throws Exception {
      Fixture fixture = fixture("NoOp");
      saveAt(fixture.noteId(), content("changed"), T1000);
      NotebookGitBinding afterChanged =
          inCommittedTransaction(
              transactionManager,
              () ->
                  notebookGitBindingRepository
                      .findByNotebook_Id(fixture.notebookId())
                      .orElseThrow());
      String head = afterChanged.getAcceptedGitObjectId();
      byte[] bundle = afterChanged.getBundleBytes();
      Timestamp eligibilityAt = afterChanged.getAmendmentLastChangedAt();

      testabilitySettings.timeTravelTo(Timestamp.from(T1008));
      textContentController.updateNoteContent(
          noteRepository.findById(fixture.noteId()).orElseThrow(), contentDto(content("changed")));

      NotebookGitBinding afterNoOp =
          inCommittedTransaction(
              transactionManager,
              () ->
                  notebookGitBindingRepository
                      .findByNotebook_Id(fixture.notebookId())
                      .orElseThrow());
      assertThat(afterNoOp.getAcceptedGitObjectId(), equalTo(head));
      assertThat(afterNoOp.getBundleBytes(), equalTo(bundle));
      assertThat(afterNoOp.getAmendmentLastChangedAt(), equalTo(eligibilityAt));

      saveAt(fixture.noteId(), content("after-noop"), Instant.parse("2026-09-08T10:09:30Z"));
      assertThat(contentChain(fixture.notebookId(), "NoOp.md").size(), is(2));
    }
  }
}
