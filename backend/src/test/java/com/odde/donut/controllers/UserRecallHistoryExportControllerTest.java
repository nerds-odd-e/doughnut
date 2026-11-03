package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.odde.donut.controllers.dto.AnswerSpellingDTO;
import com.odde.donut.controllers.dto.DailyProbeRequestDTO;
import com.odde.donut.controllers.dto.DailyProbeTrialDTO;
import com.odde.donut.controllers.dto.RecallHistoryExport;
import com.odde.donut.controllers.dto.RecallHistoryExport.DailyProbe;
import com.odde.donut.controllers.dto.RecallHistoryExport.Event;
import com.odde.donut.controllers.dto.RecallHistoryExport.EventKind;
import com.odde.donut.controllers.dto.UserDTO;
import com.odde.donut.entities.AnswerOutcome;
import com.odde.donut.entities.Grade;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.MemoryTrackerType;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.QuestionType;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.User;
import com.odde.donut.entities.UserToken;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

class UserRecallHistoryExportControllerTest extends ControllerTestBase {
  @Autowired UserController controller;
  @Autowired RecallPromptController recallPromptController;
  @Autowired MemoryTrackerController memoryTrackerController;
  @Autowired MockMvc mockMvc;
  @Autowired NoteController noteController;
  @Autowired DailyProbeController dailyProbeController;

  Note note;
  MemoryTracker tracker;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    note = ownedNote("Some Note");
    tracker = makeMe.aMemoryTrackerFor(note).please();
  }

  @Test
  void requiresLogin() {
    currentUser.setUser(null);
    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> controller.exportRecallHistory("Asia/Bangkok"));
    assertEquals(HttpStatusCode.valueOf(401), exception.getStatusCode());
  }

  @Test
  void namesTheTimezoneAndTheExportTimeInThatZone() {
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(1, 8).please());

    RecallHistoryExport export = controller.exportRecallHistory("Asia/Bangkok");

    assertThat(export.timezone(), equalTo("Asia/Bangkok"));
    assertThat(export.exportedAt(), equalTo("1989-01-02T15:00:00+07:00"));
    assertThat(export.recallEvents(), hasSize(0));
  }

  @Test
  void listsEveryKindOfRecallEventOldestFirstInTheRequestedZone()
      throws UnexpectedNoAccessRightException {
    answerMcqAtDay(1);
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(2, 8).please());
    memoryTrackerController.markAsRecalled(tracker, Grade.GOOD);
    makeMe
        .aRecallLogFor(tracker)
        .grade(Grade.HARD)
        .tutorFeedback("Mixed up the tenses")
        .recordedAt(makeMe.aTimestamp().of(3, 8).please())
        .please();

    List<Event> events = controller.exportRecallHistory("Asia/Bangkok").recallEvents();

    assertThat(
        events.stream().map(Event::kind).toList(),
        contains(
            EventKind.ANSWERED_RECALL_PROMPT, EventKind.JUST_REVIEW, EventKind.TUTOR_FEEDBACK));
    assertThat(
        events.stream().map(Event::recordedAt).toList(),
        contains(
            "1989-01-02T15:00:00+07:00", "1989-01-03T15:00:00+07:00", "1989-01-04T15:00:00+07:00"));
    assertThat(events.stream().map(Event::grade).toList(), contains("GOOD", "GOOD", "HARD"));
    assertThat(events.get(1).answer(), nullValue());
    assertThat(events.get(2).tutorFeedback(), equalTo("Mixed up the tenses"));
    assertThat(events.get(2).answer(), nullValue());
  }

  @Test
  void anAnsweredRecallPromptCarriesItsTrackerPromptAndAnswerDetails()
      throws UnexpectedNoAccessRightException {
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(1, 8).please());
    RecallPrompt prompt =
        makeMe.aRecallPrompt().withMcqForNote(note).forMemoryTracker(tracker).please();
    var answerDTO = choiceAnswer(0);
    answerDTO.setThinkingTimeMs(4200);
    answerDTO.setAwayMs(300);
    answerDTO.setAwayCount(1);
    answerDTO.setDetourMs(500);
    answerDTO.setDetourCount(2);
    answerDTO.setIdleMs(700);
    recallPromptController.answer(prompt, answerDTO);

    Event event = controller.exportRecallHistory("Asia/Bangkok").recallEvents().get(0);

    assertThat(event.memoryTrackerId(), equalTo(tracker.getId()));
    assertThat(event.memoryTrackerType(), equalTo(MemoryTrackerType.UNDERSTANDING));
    assertThat(event.noteId(), equalTo(note.getId()));
    RecallHistoryExport.Answer answer = event.answer();
    assertThat(answer.id(), equalTo(prompt.getAnswer().getId()));
    assertThat(answer.questionType(), equalTo(QuestionType.MCQ));
    assertThat(answer.promptCreatedAt(), notNullValue());
    assertThat(answer.createdAt(), equalTo("1989-01-02T15:00:00+07:00"));
    assertThat(
        List.of(
            answer.thinkingTimeMs(),
            answer.awayMs(),
            answer.awayCount(),
            answer.detourMs(),
            answer.detourCount(),
            answer.idleMs()),
        contains(4200, 300, 1, 500, 2, 700));
    assertThat(answer.excludedAt(), nullValue());
  }

  @Test
  void twoAnswersOnOneTrackerShareItsIdsAndTheSecondStartsFromTheStateTheFirstLeft()
      throws UnexpectedNoAccessRightException {
    answerMcqAtDay(1);
    makeMe.entityPersister.flush();
    makeMe.refresh(tracker);
    Float stabilityLeft = tracker.getStability();
    Float difficultyLeft = tracker.getDifficulty();
    answerMcqAtDay(3);

    List<Event> events = controller.exportRecallHistory("Asia/Bangkok").recallEvents();

    assertThat(events, hasSize(2));
    assertThat(
        events.stream().map(Event::memoryTrackerId).toList(), everyItem(equalTo(tracker.getId())));
    assertThat(events.stream().map(Event::noteId).toList(), everyItem(equalTo(note.getId())));
    assertThat(events.get(1).stabilityBefore(), equalTo(stabilityLeft));
    assertThat(events.get(1).difficultyBefore(), equalTo(difficultyLeft));
    assertThat(events.get(1).elapsedHours(), equalTo(48));
    assertThat(events.get(1).retrievability(), notNullValue());
  }

  @Test
  void anExcludedAnswerIsPresentWithItsExclusionTimeAndReason()
      throws UnexpectedNoAccessRightException {
    RecallPrompt prompt = answerMcqAtDay(1);
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(2, 8).please());
    recallPromptController.excludeAsAmbiguous(prompt, ambiguousExclusion("both tenses fit"));

    RecallHistoryExport.Answer answer =
        controller.exportRecallHistory("Asia/Bangkok").recallEvents().get(0).answer();

    assertThat(answer.excludedAt(), equalTo("1989-01-03T15:00:00+07:00"));
    assertThat(answer.exclusionReason(), equalTo("both tenses fit"));
  }

  @Test
  void anAnswerOnATrackerRemovedFromRecallIsStillExported()
      throws UnexpectedNoAccessRightException {
    answerMcqAtDay(1);
    memoryTrackerController.removeFromRepeating(tracker);

    List<Event> events = controller.exportRecallHistory("Asia/Bangkok").recallEvents();

    assertThat(events.stream().map(Event::memoryTrackerId).toList(), contains(tracker.getId()));
  }

  @Test
  void anAnswerOnANoteInTrashIsStillExportedWithThatNoteId() throws Exception {
    answerMcqAtDay(1);
    noteController.trashNote(note, leaveDeadLinks());

    List<Event> events = controller.exportRecallHistory("Asia/Bangkok").recallEvents();

    assertThat(events.stream().map(Event::noteId).toList(), contains(note.getId()));
  }

  @Test
  void anAccidentalMatchGivesAGradeAndAConfusionEventSharingTheAnswer()
      throws UnexpectedNoAccessRightException {
    MemoryTracker prompted =
        makeMe
            .aMemoryTrackerFor(ownedNote("Prompted"))
            .stabilityAndNextRecallAt(200.0f)
            .spelling()
            .please();
    MemoryTracker matched =
        makeMe
            .aMemoryTrackerFor(ownedNote("Another Note Title"))
            .stabilityAndNextRecallAt(200.0f)
            .spelling()
            .please();
    RecallPrompt prompt = makeMe.aRecallPrompt().forMemoryTracker(prompted).spelling().please();
    AnswerSpellingDTO spelling = new AnswerSpellingDTO();
    spelling.setSpellingAnswer("Another Note Title");
    recallPromptController.answerSpelling(prompt, spelling);

    List<Event> events = controller.exportRecallHistory("Asia/Bangkok").recallEvents();

    assertThat(
        events.stream().map(e -> e.memoryTrackerId() + ":" + e.grade()).toList(),
        containsInAnyOrder(prompted.getId() + ":AGAIN", matched.getId() + ":CONFUSION"));
    assertThat(
        events.stream().map(e -> e.answer().id()).toList(),
        everyItem(equalTo(prompt.getAnswer().getId())));
    assertThat(
        events.stream().map(e -> e.answer().questionType()).toList(),
        everyItem(equalTo(QuestionType.SPELLING)));
    assertThat(
        events.stream().map(e -> e.answer().outcome()).toList(),
        everyItem(equalTo(AnswerOutcome.ACCIDENTAL_MATCH)));
    assertThat(
        events.stream().map(Event::memoryTrackerType).toList(),
        everyItem(equalTo(MemoryTrackerType.SPELLING)));
  }

  @Test
  void anAgentWithTheLearnersTokenGetsTheJson() throws Exception {
    answerMcqAtDay(1);
    takeProbeAtDay(2, 3.5, 85, 2, 0.4);
    UserToken token =
        makeMe.aUserToken().forUser(currentUser.getUser()).withLabel("agent").please();

    mockMvc
        .perform(
            get("/api/user/recall-history-export")
                .param("timezone", "Asia/Bangkok")
                .header("Authorization", "Bearer " + token.getToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.timezone").value("Asia/Bangkok"))
        .andExpect(jsonPath("$.recallEvents[0].recordedAt").value("1989-01-02T15:00:00+07:00"))
        .andExpect(jsonPath("$.recallEvents[0].kind").value("ANSWERED_RECALL_PROMPT"))
        .andExpect(jsonPath("$.recallEvents[0].answer.questionType").value("MCQ"))
        .andExpect(
            jsonPath("$.recallEvents[0].answer.createdAt").value("1989-01-02T15:00:00+07:00"))
        .andExpect(jsonPath("$.dailyProbes[0].completedAt").value("1989-01-03T15:00:00+07:00"))
        .andExpect(jsonPath("$.dailyProbes[0].accuracy").value(85))
        .andExpect(jsonPath("$.dailyProbes[0].trials[0].rtMs").value(612));
  }

  @Test
  void aRequestWithoutCredentialIsRefused() throws Exception {
    currentUser.setUser(null);

    mockMvc
        .perform(get("/api/user/recall-history-export").param("timezone", "Asia/Bangkok"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void listsTheLearnersDailyProbesOldestFirstWithTheirScoresAndTrials() throws Exception {
    takeProbeAtDay(3, 3.5, 85, 2, 0.4);
    takeProbeAtDay(1, 4.0, 100, 0, 0.1);
    makeMe.aDailyProbe().by(makeMe.aUser().please()).please();

    List<DailyProbe> probes = controller.exportRecallHistory("Asia/Bangkok").dailyProbes();

    assertThat(
        probes.stream().map(DailyProbe::completedAt).toList(),
        contains("1989-01-02T15:00:00+07:00", "1989-01-04T15:00:00+07:00"));
    DailyProbe later = probes.get(1);
    assertThat(
        List.of(later.speed(), later.accuracy(), later.lapseCount(), later.variability()),
        contains(3.5, 85, 2, 0.4));
    assertThat(later.trials(), hasSize(20));
    DailyProbeTrialDTO trial = later.trials().get(0);
    assertThat(
        List.of(trial.stimulus, trial.response, trial.rtMs, trial.correct),
        contains("left", "right", 612, false));
  }

  @Test
  void probesTakenBeforeTheProbeWasTurnedOffAreStillExported() throws Exception {
    User user = currentUser.getUser();
    setDailyProbeEnabled(user, true);
    takeProbeAtDay(1, 4.0, 100, 0, 0.1);
    setDailyProbeEnabled(user, false);

    List<DailyProbe> probes = controller.exportRecallHistory("Asia/Bangkok").dailyProbes();

    assertThat(
        probes.stream().map(DailyProbe::completedAt).toList(),
        contains("1989-01-02T15:00:00+07:00"));
  }

  @Test
  void aLearnerWithoutProbesGetsRecallEventsAndAnEmptyProbeList()
      throws UnexpectedNoAccessRightException {
    answerMcqAtDay(1);

    RecallHistoryExport export = controller.exportRecallHistory("Asia/Bangkok");

    assertThat(export.recallEvents(), hasSize(1));
    assertThat(export.dailyProbes(), hasSize(0));
  }

  private void takeProbeAtDay(
      int day, double speed, int accuracy, int lapseCount, double variability) throws Exception {
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(day, 8).please());
    DailyProbeRequestDTO request = makeMe.aDailyProbe().createRequest();
    request.speed = speed;
    request.accuracy = accuracy;
    request.lapseCount = lapseCount;
    request.variability = variability;
    DailyProbeTrialDTO first = request.trials.get(0);
    first.response = "right";
    first.rtMs = 612;
    first.correct = false;
    dailyProbeController.createDailyProbe(request);
  }

  private void setDailyProbeEnabled(User user, boolean enabled)
      throws UnexpectedNoAccessRightException {
    UserDTO dto = new UserDTO();
    dto.setName(user.getName());
    dto.setDailyAssimilationCount(user.getDailyAssimilationCount());
    dto.setDailyProbeEnabled(enabled);
    controller.updateUser(user, dto);
  }

  private Note ownedNote(String title) {
    return makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title(title).please();
  }

  private RecallPrompt answerMcqAtDay(int day) throws UnexpectedNoAccessRightException {
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(day, 8).please());
    RecallPrompt prompt =
        makeMe.aRecallPrompt().withMcqForNote(note).forMemoryTracker(tracker).please();
    recallPromptController.answer(prompt, choiceAnswer(0));
    return prompt;
  }
}
