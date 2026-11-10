package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.RecallLog;
import com.odde.donut.entities.repositories.NotePropertyIndexRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.NotebookGitLiveRecovery.Review;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookGitLiveRecoveryControllerTest extends NotebookGitWebContentControllerTestBase {
  static final String LIVE =
      "---\ntype: Note\ntopic: recovered\nrelated: \"[[Target]]\"\n---\nreviewed live body";
  @Autowired NotePropertyIndexRepository propertyIndexes;

  @Test
  void acceptsExactlyTheReviewedLiveTreeAndRetainsHistoryIdentitiesAndLearning() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    var folder = makeMe.aFolder().notebook(notebook).name("Ideas").please();
    Note note = makeMe.aNote().folder(folder).title("Original").content(NOTE).please();
    snapshotCurrentPortableTree(notebook);
    NoteUpdateContentDTO update = new NoteUpdateContentDTO();
    update.setContent(NOTE + " second accepted commit");
    textContentController.updateNoteContent(
        noteRepository.findById(note.getId()).orElseThrow(), update);
    var before = acceptedHistory(notebook);
    int[] learning =
        inCommittedTransaction(
            transactionManager,
            () -> {
              MemoryTracker tracker =
                  makeMe
                      .aMemoryTrackerFor(noteRepository.findById(note.getId()).orElseThrow())
                      .recallCount(3)
                      .please();
              RecallLog log = makeMe.aRecallLogFor(tracker).please();
              noteRepository.findById(note.getId()).orElseThrow().setContent(LIVE);
              return new int[] {tracker.getId(), log.getId()};
            });
    int recallCountBefore =
        inCommittedTransaction(
            transactionManager,
            () -> entityManager.find(MemoryTracker.class, learning[0]).getRecallCount());
    var newFolder = makeMe.aFolder().notebook(notebook).name("New").please();
    Note added =
        makeMe.aNote().folder(newFolder).title("Extracted").content(NOTE + " live-only").please();
    var report = controller.diagnoseNotebookGit(notebook.getId());
    Timestamp now = Timestamp.from(Instant.parse("2030-01-01T00:00:00Z"));
    testabilitySettings.timeTravelTo(now);

    String accepted =
        controller.recoverNotebookGit(
            notebook.getId(), new Review(report.acceptedHead(), report.liveTree()));

    var after = acceptedHistory(notebook);
    assertThat(before.commits().size(), greaterThan(1));
    assertThat(after.commits().getFirst(), equalTo(accepted));
    assertThat(after.parents(), equalTo(before.commits()));
    assertThat(after.tipTreeId().name(), equalTo(report.liveTree()));
    assertThat(controller.diagnoseNotebookGit(notebook.getId()).differences(), empty());
    assertThat(
        acceptedBytesAt(notebook, "Ideas/Original.md"),
        equalTo(LIVE.getBytes(StandardCharsets.UTF_8)));
    assertThat(
        noteRepository.findById(note.getId()).orElseThrow().getFolder().getId(),
        equalTo(folder.getId()));
    assertThat(
        noteRepository.findById(added.getId()).orElseThrow().getFolder().getId(),
        equalTo(newFolder.getId()));
    inCommittedTransaction(
        transactionManager,
        () -> {
          assertThat(
              entityManager.find(MemoryTracker.class, learning[0]).getNote().getId(),
              equalTo(note.getId()));
          assertThat(
              entityManager.find(MemoryTracker.class, learning[0]).getRecallCount(),
              equalTo(recallCountBefore));
          assertThat(
              entityManager.find(RecallLog.class, learning[1]).getMemoryTracker().getId(),
              equalTo(learning[0]));
          assertThat(
              propertyIndexes
                  .findByNote_IdAndPropertyKeyAndPropertyValue(note.getId(), "topic", "")
                  .getFirst()
                  .getChangedAt(),
              equalTo(now));
          assertThat(
              propertyIndexes
                  .findByNote_IdAndPropertyKeyAndPropertyValue(note.getId(), "related", "")
                  .getFirst()
                  .getAuthoredNoteReference(),
              notNullValue());
        });
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void refusesReviewWhenEitherAcceptedOrLiveStateHasChanged(boolean acceptedChanged)
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("note").content(NOTE).please();
    snapshotCurrentPortableTree(notebook);
    var reviewed = controller.diagnoseNotebookGit(notebook.getId());
    if (acceptedChanged) {
      NoteUpdateContentDTO update = new NoteUpdateContentDTO();
      update.setContent(NOTE + " accepted");
      textContentController.updateNoteContent(
          noteRepository.findById(note.getId()).orElseThrow(), update);
    } else {
      inCommittedTransaction(
          transactionManager,
          () -> noteRepository.findById(note.getId()).orElseThrow().setContent(LIVE));
    }
    String before = reloadCommittedBinding(notebook.getId()).getAcceptedGitObjectId();

    var failure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                controller.recoverNotebookGit(
                    notebook.getId(), new Review(reviewed.acceptedHead(), reviewed.liveTree())));

    assertThat(failure.getStatusCode(), equalTo(HttpStatus.CONFLICT));
    assertThat(failure.getReason(), containsString("review the current accepted and live state"));
    assertThat(reloadCommittedBinding(notebook.getId()).getAcceptedGitObjectId(), equalTo(before));
  }

  @Test
  void serializesTwoRecoveryRequestsAndRefusesTheSecondStaleReview() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("live").content(NOTE).please();
    var report = controller.diagnoseNotebookGit(notebook.getId());
    Review review = new Review(report.acceptedHead(), report.liveTree());
    var result =
        NotebookGitConcurrentWriterTestSupport.runInQueuedOrder(
            transactionManager,
            notebookGitBindingRepository,
            currentUser,
            currentUser.getUser(),
            notebook.getId(),
            () -> controller.recoverNotebookGit(notebook.getId(), review),
            () ->
                assertThrows(
                    ResponseStatusException.class,
                    () -> controller.recoverNotebookGit(notebook.getId(), review)));
    assertThat(
        reloadCommittedBinding(notebook.getId()).getAcceptedGitObjectId(), equalTo(result.first()));
    assertThat(result.second().getStatusCode(), equalTo(HttpStatus.CONFLICT));
  }

  @Test
  void refusesANonOwner() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    var report = controller.diagnoseNotebookGit(notebook.getId());
    currentUser.setUser(createFixtureUser());
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () ->
            controller.recoverNotebookGit(
                notebook.getId(), new Review(report.acceptedHead(), report.liveTree())));
  }
}
