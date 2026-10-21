package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static com.odde.donut.testability.CommittedUserCleanup.deleteByUserExternalIdentifierLike;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.controllers.dto.AnswerDTO;
import com.odde.donut.controllers.dto.AnsweredQuestion;
import com.odde.donut.entities.Answer;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.RecallPromptRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class RecallPromptAnswerRecoveryTestBase extends RecallPromptControllerTestBase {
  @Autowired PlatformTransactionManager transactionManager;
  @Autowired EntityManager entityManager;
  @Autowired RecallPromptRepository repository;
  @Autowired ObjectMapper objectMapper;

  final String fixtureUser = "recall-answer-recovery-" + UUID.randomUUID();

  @BeforeEach
  void committedOwner() {
    String setupUser = currentUser.getUser().getExternalIdentifier();
    inCommittedTransaction(
        transactionManager, () -> deleteByUserExternalIdentifierLike(entityManager, setupUser));
    currentUser.setUser(
        inCommittedTransaction(transactionManager, () -> makeMe.aUser(fixtureUser).please()));
  }

  @AfterEach
  void cleanup() {
    inCommittedTransaction(
        transactionManager,
        () -> {
          List<Integer> answerIds =
              entityManager
                  .createQuery(
                      "select rp.answer.id from RecallPrompt rp where rp.memoryTracker.user.externalIdentifier = :owner and rp.answer is not null",
                      Integer.class)
                  .setParameter("owner", fixtureUser)
                  .getResultList();
          deleteByUserExternalIdentifierLike(entityManager, fixtureUser);
          answerIds.forEach(id -> entityManager.remove(entityManager.find(Answer.class, id)));
        });
  }

  RecallPrompt createPrompt(boolean property) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          currentUser.setUser(entityManager.find(User.class, currentUser.getUser().getId()));
          Note note = ownedNote();
          MemoryTracker tracker =
              property
                  ? makeMe
                      .aMemoryTrackerFor(note)
                      .propertyKey("topic")
                      .stabilityAndNextRecallAt(200.0f)
                      .please()
                  : ownedTracker(note);
          testabilitySettings.timeTravelTo(tracker.getNextRecallAt());
          return mcqPrompt(tracker, note);
        });
  }

  AnswerDTO timedAnswer(int choice) {
    AnswerDTO dto = choiceAnswer(choice);
    dto.setThinkingTimeMs(5000);
    dto.setAwayMs(1000);
    dto.setAwayCount(2);
    dto.setDetourMs(2000);
    dto.setDetourCount(3);
    dto.setIdleMs(500);
    return dto;
  }

  AnsweredQuestion accept(RecallPrompt prompt, AnswerDTO dto) {
    try {
      return controller.answer(prompt, dto);
    } catch (UnexpectedNoAccessRightException exception) {
      throw new AssertionError(exception);
    }
  }

  Evidence evidence(int promptId) {
    return inCommittedTransaction(
        transactionManager, () -> snapshot(repository.findById(promptId).orElseThrow()));
  }

  Evidence snapshot(RecallPrompt prompt) {
    MemoryTracker tracker = prompt.requireMemoryTracker();
    return new Evidence(
        objectMapper.valueToTree(prompt.getAnswer()),
        prompt.getAnswer().getCreatedAt(),
        tracker.getStability(),
        tracker.getDifficulty(),
        tracker.getLastRecalledAt(),
        tracker.getNextRecallAt(),
        tracker.getRecallCount(),
        objectMapper.valueToTree(
            recallLogRepository.findAllByMemoryTracker_IdOrderByRecordedAtAscIdAsc(
                tracker.getId())));
  }

  record Evidence(
      JsonNode answer,
      Timestamp createdAt,
      Float stability,
      Float difficulty,
      Timestamp lastRecalledAt,
      Timestamp nextRecallAt,
      Integer recallCount,
      JsonNode logs) {}
}
