package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.controllers.dto.RecordLearningSessionResponse;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LearningSessionRecordTests extends LearningSessionControllerTestBase {

  @Test
  void recordsMatchedGradesAsRecallLogsAndSchedulesTrackers()
      throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);

    RecordLearningSessionResponse response =
        controller.record(
            recordRequest(fixture.notebook(), HOLA4_GRACIAS1_REPORT), "Asia/Shanghai");

    assertThat(response.getRecordedAt(), equalTo(dayTwo));
    assertThat(response.getRecordedItems(), hasSize(2));
    assertThat(response.getRejectedEntries(), empty());

    for (MemoryTracker tracker : List.of(fixture.holaTracker(), fixture.graciasTracker())) {
      assertThat(
          recallLogRepository.findAllByMemoryTracker_IdOrderByRecordedAtDescIdDesc(tracker.getId()),
          hasSize(1));
      assertThat(tracker.getLastRecalledAt(), equalTo(dayTwo));
    }
  }

  @Test
  void allLinesRejectedWritesNoRecallLogs() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);
    MemoryTracker holaTracker = fixture.holaTracker();
    TrackerLearningState trackerStateBefore = learningStateOf(holaTracker);
    long logsBefore = recallLogRepository.count();

    RecordLearningSessionResponse response =
        controller.record(
            recordRequest(
                fixture.notebook(),
                """
                # Learning Session Report

                UnknownNote: 3
                Hola: six
                """),
            "Asia/Shanghai");

    assertThat(response.getRecordedItems(), empty());
    assertThat(response.getRejectedEntries(), hasSize(2));
    assertThat(recallLogRepository.count(), equalTo(logsBefore));
    assertThat(learningStateOf(holaTracker), equalTo(trackerStateBefore));
  }

  @Test
  void rejectsTitleWithoutCommissionedTracker() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    Notebook notebook =
        makeMe
            .aNotebook()
            .creatorAndOwner(currentUser.getUser())
            .name("Spanish conversation")
            .please();
    makeMe.aNote().notebook(notebook).title("Hola").content("Hello").please();

    RecordLearningSessionResponse response =
        controller.record(
            recordRequest(notebook, learningSessionReport("Hola", 4)), "Asia/Shanghai");

    assertThat(response.getRecordedItems(), empty());
    assertThat(response.getRejectedEntries(), hasSize(1));
    assertThat(
        response.getRejectedEntries().getFirst().getReason(),
        containsString("No commissioned memory tracker"));
  }

  @ParameterizedTest
  @CsvSource({"_trash, ", "_TRASH, ", "_trash, sub"})
  void rejectsNoteBeneathRootTrashWithoutChangingItsLearningHistoryOrSchedule(
      String rootFolderName, String childName) throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    Folder root = makeMe.aFolder().notebook(notebook).name(rootFolderName).please();
    Folder home =
        childName == null ? root : makeMe.aFolder().parentFolder(root).name(childName).please();
    MemoryTracker tracker = commissionedNoteIn(home, "Hola", dayTwo);
    TrackerLearningState trackerStateBefore = learningStateOf(tracker);

    RecordLearningSessionResponse response =
        controller.record(
            recordRequest(notebook, learningSessionReport("Hola", 4)), "Asia/Shanghai");

    assertThat(response.getRecordedItems(), empty());
    assertThat(
        response.getRejectedEntries().getFirst().getReason(),
        containsString("No commissioned memory tracker"));
    assertThat(recallLogRepository.count(), equalTo(0L));
    assertThat(learningStateOf(tracker), equalTo(trackerStateBefore));
  }

  @Test
  void recordsAvailableNoteWhenATrashedNoteHasTheSameTitle()
      throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    Note availableNote =
        makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("なにしろ").please();
    makeMe.refresh(
        makeMe.aNote().notebook(availableNote.getNotebook()).title("なにしろ").trashed().please());
    makeMe.aMemoryTrackerFor(availableNote).commissioned().nextRecallAt(dayTwo).please();

    RecordLearningSessionResponse response =
        controller.record(
            recordRequest(
                availableNote.getNotebook(),
                sessionItemFeedbackReport(
                    "なにしろ",
                    2,
                    "「とにかく」と言い換えられることは正しく理解でき、自分でも「何しろ時間がないから…」という文を作れました。一方、最後の意味確認では「話のテーマを転換する表現」と捉えてしまい、「特に重要な事情・理由を取り上げて強調する」という核心についてもう一度説明が必要でした。使い方はかなり掴めていますが、意味の定義はまだ少し不安定です。")),
            "Asia/Shanghai");

    assertThat(response.getRecordedItems(), hasSize(1));
    assertThat(response.getRecordedItems().getFirst().getNoteTitle(), equalTo("なにしろ"));
    assertThat(response.getRejectedEntries(), empty());
  }

  @Test
  void rejectsAmbiguousTitleWhenTwoAvailableNotesInDifferentFoldersShareIt()
      throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    Folder greetings = makeMe.aFolder().notebook(notebook).name("greetings").please();
    Folder slang = makeMe.aFolder().notebook(notebook).name("slang").please();
    MemoryTracker greetingsTracker = commissionedNoteIn(greetings, "Hola", dayTwo);
    MemoryTracker slangTracker = commissionedNoteIn(slang, "Hola", dayTwo);
    TrackerLearningState greetingsStateBefore = learningStateOf(greetingsTracker);
    TrackerLearningState slangStateBefore = learningStateOf(slangTracker);

    RecordLearningSessionResponse response =
        controller.record(
            recordRequest(notebook, learningSessionReport("Hola", 4)), "Asia/Shanghai");

    assertThat(response.getRecordedItems(), empty());
    assertThat(
        response.getRejectedEntries().getFirst().getReason(),
        containsString("Ambiguous note title in notebook."));
    assertThat(recallLogRepository.count(), equalTo(0L));
    assertThat(learningStateOf(greetingsTracker), equalTo(greetingsStateBefore));
    assertThat(learningStateOf(slangTracker), equalTo(slangStateBefore));
  }

  @Test
  void recordsCommissionedNoteInAFolderNamedTrashBeneathAnOrdinaryRoot()
      throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    Folder docs = makeMe.aFolder().notebook(notebook).name("docs").please();
    Folder nestedTrash = makeMe.aFolder().parentFolder(docs).name("_trash").please();
    MemoryTracker tracker = commissionedNoteIn(nestedTrash, "Hola", dayTwo);

    RecordLearningSessionResponse response =
        controller.record(
            recordRequest(notebook, learningSessionReport("Hola", 4)), "Asia/Shanghai");

    assertThat(response.getRejectedEntries(), empty());
    assertThat(
        response.getRecordedItems().getFirst().getMemoryTrackerId(), equalTo(tracker.getId()));
  }

  @Test
  void legacyScoresTagReportRecordsGrades() throws UnexpectedNoAccessRightException {
    Timestamp dayTwo = makeMe.aTimestamp().of(1, 9).please();
    testabilitySettings.timeTravelTo(dayTwo);

    SpanishNotebookFixture fixture = spanishNotebookFixture(dayTwo);
    RecordLearningSessionResponse response =
        controller.record(
            recordRequest(fixture.notebook(), legacyScoresTaggedReport("Hola: 4\nGracias: 1\n")),
            "Asia/Shanghai");

    assertThat(response.getRejectedEntries(), empty());
    assertThat(response.getRecordedItems(), hasSize(2));
    assertThat(response.getRecordedItems().get(0).getGrade(), equalTo(4));
    assertThat(response.getRecordedItems().get(1).getGrade(), equalTo(1));
    assertThat(fixture.holaTracker().getLastRecalledAt(), equalTo(dayTwo));
  }

  private MemoryTracker commissionedNoteIn(Folder folder, String title, Timestamp dueAt) {
    Note note = makeMe.aNote().folder(folder).title(title).please();
    return makeMe.aMemoryTrackerFor(note).commissioned().nextRecallAt(dueAt).please();
  }

  private static TrackerLearningState learningStateOf(MemoryTracker tracker) {
    return new TrackerLearningState(
        tracker.getLastRecalledAt(),
        tracker.getRecallCount(),
        tracker.getStability(),
        tracker.getDifficulty(),
        tracker.getNextRecallAt());
  }

  private record TrackerLearningState(
      Timestamp lastRecalledAt,
      Integer recallCount,
      Float stability,
      Float difficulty,
      Timestamp nextRecallAt) {}
}
