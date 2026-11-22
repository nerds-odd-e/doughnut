package com.odde.donut.controllers;

import static com.odde.donut.controllers.dto.Randomization.RandomStrategy.first;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.controllers.dto.Randomization;
import com.odde.donut.controllers.dto.RecallHistoryItem;
import com.odde.donut.entities.Grade;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.RecallLog;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.services.notebookGit.NotebookGitHistoryService;
import com.odde.donut.testability.OpenAiStructuredResponseMock;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class MemoryTrackerRecallPromptFreshnessControllerTest extends MemoryTrackerControllerTestBase {
  private static final String ORIGINAL_TITLE = "Observatory";
  private static final String ORIGINAL_SITE = "North ridge";
  private static final String ORIGINAL_BODY = "The observatory opens at nine.";
  private static final String CORRECTED_BODY = "The observatory opens at ten.";
  private static final String WAITING_STEM = "When did the observatory open?";
  private static final String CURRENT_STEM = "What does the current observatory note say?";

  @Autowired EntityManager entityManager;
  @Autowired TextContentController textContentController;
  @Autowired NotebookGitHistoryService notebookGitHistory;
  @Autowired NotebookGitBindingRepository notebookGitBindings;

  OpenAiStructuredResponseMock openAi;
  MemoryTracker tracker;

  @BeforeEach
  void prepareRecall() {
    openAi = new OpenAiStructuredResponseMock(officialClient);
    testabilitySettings.setRandomization(new Randomization(first, 0));
    var note =
        makeMe
            .aNote(ORIGINAL_TITLE)
            .notebookOwnedBy(currentUser.getUser())
            .content(markdown(ORIGINAL_SITE, ORIGINAL_BODY))
            .please();
    notebookGitHistory.resetHistory(note.getNotebook(), Instant.parse("2025-01-01T00:00:00Z"));
    tracker = ownedTracker(note);
  }

  @ParameterizedTest
  @CsvSource({
    "North ridge, The observatory opens at ten., Ten",
    "North ridge, The observatory opens at nine!, Nine",
    "South ridge, The observatory opens at nine., South ridge"
  })
  void replacesWaitingQuestionAfterAnAcceptedMarkdownEdit(
      String site, String body, String correctChoice) throws Exception {
    var waiting = loadWaitingQuestion();
    String initialAcceptedRevision = acceptedRevision();

    editContent(site, body);

    assertRenewedQuestion(
        waiting.getId(),
        initialAcceptedRevision,
        ORIGINAL_TITLE,
        markdown(site, body),
        correctChoice);
  }

  @Test
  void replacesWaitingQuestionAfterAnAcceptedTitleEdit() throws Exception {
    String title = "Mountain Observatory";
    var waiting = loadWaitingQuestion();
    String initialAcceptedRevision = acceptedRevision();

    textContentController.updateNoteTitle(tracker.getNote(), titleDto(title));

    assertRenewedQuestion(
        waiting.getId(),
        initialAcceptedRevision,
        title,
        markdown(ORIGINAL_SITE, ORIGINAL_BODY),
        title);
  }

  private void assertRenewedQuestion(
      int waitingId,
      String initialAcceptedRevision,
      String title,
      String content,
      String correctChoice)
      throws Exception {
    assertThat(acceptedRevision(), not(equalTo(initialAcceptedRevision)));
    String originalInput = openAi.requestInputTexts(1).getFirst();
    assertThat(originalInput, containsString(markdown(ORIGINAL_SITE, ORIGINAL_BODY)));
    reloadTracker();
    serveQuestion(CURRENT_STEM, correctChoice);

    var renewed = controller.getRecallPrompt(tracker);

    assertThat(renewed.getId(), not(equalTo(waitingId)));
    assertThat(renewed.getMcq().getQuestionStem(), equalTo(CURRENT_STEM));
    assertThat(
        renewed.getMcq().getResponseChoices(),
        equalTo(List.of(correctChoice, "distractor1", "distractor2", "distractor3")));
    assertThat(controller.getRecallPrompt(tracker).getId(), equalTo(renewed.getId()));
    String input = openAi.requestInputTexts(2).getLast();
    assertThat(input, containsString("<focus_note>\nTitle: " + title + "\n"));
    assertThat(input, containsString(content));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"{\"questionStem\":\"When?\",\"responseChoices\":[\"Nine\",\"Ten\"]}", "{legacy"})
  void renewsQuestionWithUnknownGenerationInput(String storedQuestion) throws Exception {
    RecallPrompt legacy = promptFor(tracker);
    makeMe.entityPersister.flush();
    entityManager
        .createNativeQuery("UPDATE mcq SET raw_json_question = :question WHERE id = :id")
        .setParameter("question", storedQuestion)
        .setParameter("id", legacy.getMcq().getId())
        .executeUpdate();
    reloadTracker();
    serveQuestion(CURRENT_STEM, "Nine");

    var renewed = controller.getRecallPrompt(tracker);

    assertThat(renewed.getId(), not(equalTo(legacy.getId())));
    assertThat(renewed.getMcq().getQuestionStem(), equalTo(CURRENT_STEM));
    assertThat(openAi.requestInputTexts(1).getFirst(), containsString(ORIGINAL_BODY));
  }

  @Test
  void failedRenewalAndRetryPreservePriorRecallState() throws Exception {
    RecallPrompt earlier = answeredPromptFor(tracker);
    var earlierAnswerId = earlier.getAnswer().getId();
    var earlierChoices = earlier.getMcq().getResponseChoices();
    var previousLog =
        makeMe.aRecallLogFor(tracker).answer(earlier.getAnswer()).grade(Grade.GOOD).please();
    var dueAt = tracker.getNextRecallAt();
    var waiting = loadWaitingQuestion();

    editContent(ORIGINAL_SITE, CORRECTED_BODY);
    reloadTracker();
    var unavailable =
        assertThrows(ResponseStatusException.class, () -> controller.getRecallPrompt(tracker));
    assertThat(unavailable.getStatusCode(), equalTo(HttpStatus.SERVICE_UNAVAILABLE));
    assertThat(controller.showMemoryTracker(tracker).getNextRecallAt(), equalTo(dueAt));
    assertThat(
        recallLogsOf(tracker).stream().map(RecallLog::getId).toList(),
        contains(previousLog.getId()));

    serveQuestion(CURRENT_STEM, "Ten");
    var renewed = controller.getRecallPrompt(tracker);
    reloadTracker();
    assertThat(renewed.getId(), not(equalTo(waiting.getId())));
    assertThat(renewed.getMcq().getQuestionStem(), equalTo(CURRENT_STEM));
    assertThat(controller.showMemoryTracker(tracker).getNextRecallAt(), equalTo(dueAt));
    assertThat(
        recallLogsOf(tracker).stream().map(RecallLog::getId).toList(),
        contains(previousLog.getId()));
    assertThat(
        entityManager.find(RecallPrompt.class, renewed.getId()).getMemoryTracker().getId(),
        equalTo(tracker.getId()));
    var earlierHistory = historyFor(earlier.getId()).getRecallPrompt();
    assertThat(earlierHistory.getAnswer().getId(), equalTo(earlierAnswerId));
    assertThat(earlierHistory.getAnswer().getChoiceIndex(), equalTo(0));
    assertThat(earlierHistory.getMcq().getResponseChoices(), equalTo(earlierChoices));
    assertThat(openAi.requestInputTexts(3).getLast(), containsString(CORRECTED_BODY));
  }

  private RecallHistoryItem historyFor(int promptId) throws Exception {
    return controller.getRecallHistory(tracker).stream()
        .filter(
            item -> item.getRecallPrompt() != null && item.getRecallPrompt().getId() == promptId)
        .findFirst()
        .orElseThrow();
  }

  private void reloadTracker() {
    makeMe.entityPersister.flushAndClear();
    tracker = entityManager.find(MemoryTracker.class, tracker.getId());
  }

  private String acceptedRevision() {
    return notebookGitBindings.findAcceptedRevision(tracker.getNote().getNotebook().getId());
  }

  private void editContent(String site, String body) throws Exception {
    var edit = new NoteUpdateContentDTO();
    edit.setContent(markdown(site, body));
    textContentController.updateNoteContent(tracker.getNote(), edit);
  }

  private static String markdown(String site, String body) {
    return "---\ntype: Note\nsite: " + site + "\n---\n" + body;
  }

  private com.odde.donut.controllers.dto.RecallPrompt loadWaitingQuestion() throws Exception {
    serveQuestion(WAITING_STEM, "Nine");
    return controller.getRecallPrompt(tracker);
  }

  private void serveQuestion(String stem, String correctChoice) {
    openAi.stubStructuredResponse(
        makeMe.aGeneratedMcq().stem(stem).correctAnswer(correctChoice).please());
  }
}
