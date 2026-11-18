package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static com.odde.donut.testability.CommittedUserCleanup.deleteByUserExternalIdentifierLike;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.controllers.dto.Randomization;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.services.ai.GeneratedMcq;
import com.odde.donut.services.notebookGit.NotebookGitHistoryService;
import com.odde.donut.testability.OpenAiStructuredResponseMock;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MemoryTrackerRecallPromptRaceControllerTest extends ControllerTestBase {
  private static final String ORIGINAL_CONTENT =
      "---\ntype: Note\n---\nThe observatory opens at nine.";
  private static final String EDITED_CONTENT =
      "---\ntype: Note\n---\nThe observatory opens at ten.";

  @Autowired MemoryTrackerController controller;
  @Autowired RecallPromptController recallPromptController;
  @Autowired TextContentController textContentController;
  @Autowired NotebookGitBindingRepository notebookGitBindings;
  @Autowired NotebookGitHistoryService notebookGitHistory;
  @Autowired PlatformTransactionManager transactionManager;
  @Autowired EntityManager entityManager;

  private final String userIdentifier = "recall-prompt-race-" + UUID.randomUUID();
  private final List<Boolean> providerTransactions = new ArrayList<>();
  private MemoryTracker tracker;
  private OpenAiStructuredResponseMock openAi;

  @BeforeEach
  void prepareAcceptedFocus() {
    openAi = new OpenAiStructuredResponseMock(officialClient);
    testabilitySettings.setRandomization(new Randomization(Randomization.RandomStrategy.first, 0));
    tracker =
        inCommittedTransaction(
            transactionManager,
            () -> {
              currentUser.setUser(makeMe.aUser(userIdentifier).please());
              Note note =
                  makeMe
                      .aNote("Observatory")
                      .notebookOwnedBy(currentUser.getUser())
                      .content(ORIGINAL_CONTENT)
                      .please();
              notebookGitHistory.resetHistory(
                  note.getNotebook(), Instant.parse("2025-01-01T00:00:00Z"));
              return makeMe.aMemoryTrackerFor(note).please();
            });
  }

  @AfterEach
  void cleanupCommittedState() {
    inCommittedTransaction(
        transactionManager,
        () -> deleteByUserExternalIdentifierLike(entityManager, mcqRepository, userIdentifier));
  }

  @Test
  void retriesWithTheAcceptedEditWhenTheCompetingQuestionIsStale() throws Exception {
    String originalRevision = acceptedRevision();
    long questionCountBefore = mcqRepository.count();
    providerCallRacingWith(() -> {
      insertCompetingPrompt(tracker.getNote(), "Question from the original note", "Nine");
      commitEdit();
    });

    var staleLoad =
        assertThrows(ResponseStatusException.class, () -> controller.getRecallPrompt(tracker));

    assertThat(staleLoad.getStatusCode(), equalTo(HttpStatus.SERVICE_UNAVAILABLE));
    assertThat(acceptedRevision(), not(equalTo(originalRevision)));
    assertThat(mcqRepository.count(), equalTo(questionCountBefore + 1));

    openAi.stubStructuredResponse(generatedQuestion("Question from the corrected note", "Ten"));
    var current = controller.getRecallPrompt(reloadTracker());

    assertThat(current.getMcq().getQuestionStem(), equalTo("Question from the corrected note"));
    assertThat(current.getMcq().getResponseChoices().getFirst(), equalTo("Ten"));
    assertThat(
        openAi.requestInputTexts(2),
        contains(containsString(ORIGINAL_CONTENT), containsString(EDITED_CONTENT)));
    assertThat(providerTransactions, contains(false, false));

    var answer = recallPromptController.answer(reloadPrompt(current.getId()), choiceAnswer(0));
    assertThat(answer.getId(), equalTo(current.getId()));
    assertThat(answer.getMemoryTrackerId(), equalTo(tracker.getId()));
    assertThat(answer.getAnswer().getCorrect(), is(true));
    var persisted = reloadPrompt(current.getId());
    assertThat(persisted.getMcq().getId(), equalTo(current.getMcq().getId()));
    assertThat(persisted.getAnswer().getChoiceIndex(), equalTo(0));
  }

  @Test
  void reusesTheMatchingPromptInsertedAfterAnEditDuringGeneration() throws Exception {
    long questionCountBefore = mcqRepository.count();
    AtomicReference<Integer> competingPromptId = new AtomicReference<>();
    providerCallRacingWith(() -> {
      commitEdit();
      competingPromptId.set(
          insertCompetingPrompt(
              reloadTracker().getNote(), "Question from the concurrent generation", "Ten"));
    });

    var selected = controller.getRecallPrompt(tracker);

    assertThat(selected.getId(), equalTo(competingPromptId.get()));
    assertThat(selected.getMcq().getResponseChoices().getFirst(), equalTo("Ten"));
    assertThat(mcqRepository.count(), equalTo(questionCountBefore + 1));
    assertThat(openAi.requestInputTexts(1).getFirst(), containsString(ORIGINAL_CONTENT));
    assertThat(providerTransactions, contains(false));
  }

  private void providerCallRacingWith(Runnable concurrentChange) {
    openAi.stubStructuredResponse(
        generatedQuestion("Question generated from the original note", "Nine"));
    openAi.onBeforeCreate(() -> {
      providerTransactions.add(TransactionSynchronizationManager.isActualTransactionActive());
      openAi.onBeforeCreate(() ->
          providerTransactions.add(TransactionSynchronizationManager.isActualTransactionActive()));
      concurrentChange.run();
    });
  }

  private void commitEdit() {
    var edit = new NoteUpdateContentDTO();
    edit.setContent(EDITED_CONTENT);
    assertDoesNotThrow(() -> textContentController.updateNoteContent(tracker.getNote(), edit));
  }

  private Integer insertCompetingPrompt(Note focus, String stem, String correctChoice) {
    return inCommittedTransaction(
        transactionManager,
        () ->
            makeMe
                .aRecallPrompt()
                .forMemoryTracker(tracker)
                .withMcq(
                    makeMe
                        .anMcq()
                        .stem(stem)
                        .choices(correctChoice, "Noon")
                        .generatedFrom(focus)
                        .inMemoryPlease())
                .please()
                .getId());
  }

  private String acceptedRevision() {
    return notebookGitBindings.findAcceptedRevision(tracker.getNote().getNotebook().getId());
  }

  private MemoryTracker reloadTracker() {
    return entityManager.find(MemoryTracker.class, tracker.getId());
  }

  private RecallPrompt reloadPrompt(Integer promptId) {
    return entityManager.find(RecallPrompt.class, promptId);
  }

  private GeneratedMcq generatedQuestion(String stem, String correctChoice) {
    return makeMe.aGeneratedMcq().stem(stem).correctAnswer(correctChoice).please();
  }
}
