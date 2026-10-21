package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static com.odde.donut.testability.CommittedUserCleanup.deleteByUserExternalIdentifierLike;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.AnswerDTO;
import com.odde.donut.controllers.dto.AnsweredQuestion;
import com.odde.donut.entities.Grade;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.server.ResponseStatusException;

class RecallPromptAnswerRecoveryControllerTest extends RecallPromptAnswerRecoveryTestBase {
  @ParameterizedTest
  @CsvSource({"0, false", "1, false", "0, true"})
  void retriesRecoverOriginalAnswerAndAllLearningEvidence(int choice, boolean property) {
    RecallPrompt prompt = createPrompt(property);
    AnswerDTO first = timedAnswer(choice);
    Integer answerId =
        inCommittedTransaction(
            transactionManager,
            () ->
                accept(repository.findById(prompt.getId()).orElseThrow(), first)
                    .getAnswer()
                    .getId());
    Evidence original = evidence(prompt.getId());
    assertThat(original.answer().get("id").intValue(), equalTo(answerId));
    assertThat(original.answer().get("choiceIndex").intValue(), equalTo(choice));
    assertThat(original.answer().get("correct").booleanValue(), is(choice == 0));
    assertThat(original.tracker().logs().size(), equalTo(1));
    assertThat(
        original.tracker().logs().get(0).get("productOutcome").textValue(),
        equalTo((choice == 0 ? Grade.GOOD : Grade.AGAIN).name()));
    assertThat(original.tracker().logs().get(0).get("answerId").intValue(), equalTo(answerId));
    assertThat(original.tracker().recallCount(), equalTo(1));
    testabilitySettings.timeTravelTo(new Timestamp(original.createdAt().getTime() + 86_400_000));

    for (int retryChoice : new int[] {choice, 1 - choice, choice}) {
      inCommittedTransaction(
          transactionManager,
          () -> {
            AnsweredQuestion recovered =
                accept(
                    repository.findById(prompt.getId()).orElseThrow(), choiceAnswer(retryChoice));
            assertThat(objectMapper.valueToTree(recovered.getAnswer()), equalTo(original.answer()));
            assertThat(recovered.getAnswer().getCreatedAt(), equalTo(original.createdAt()));
          });
    }

    assertThat(evidence(prompt.getId()), equalTo(original));
  }

  @Test
  void recoveryRequiresTheOriginalOwnerWithoutChangingLearningEvidence() {
    RecallPrompt prompt = createPrompt(false);
    inCommittedTransaction(transactionManager, () -> accept(prompt, choiceAnswer(0)));
    Evidence original = evidence(prompt.getId());
    User owner = currentUser.getUser();
    currentUser.setUser(null);
    assertThrows(ResponseStatusException.class, () -> controller.answer(prompt, choiceAnswer(1)));
    User other =
        inCommittedTransaction(
            transactionManager, () -> makeMe.aUser(fixtureUser + "-other").please());
    try {
      currentUser.setUser(other);
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.answer(prompt, choiceAnswer(1)));
      assertThat(evidence(prompt.getId()), equalTo(original));
    } finally {
      currentUser.setUser(owner);
      inCommittedTransaction(
          transactionManager,
          () -> deleteByUserExternalIdentifierLike(entityManager, other.getExternalIdentifier()));
    }
  }
}
