package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.odde.doughnut.entities.QuestionGenerationBatchUserState;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchUserStateRepository;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionGenerationBatchPlanningServiceTest {

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchPlanningService planningService;
  @Autowired QuestionGenerationBatchUserStateRepository userStateRepository;

  User user;
  Timestamp currentTime;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    currentTime = makeMe.aTimestamp().of(10, 8).fromShanghai().please();
  }

  @Test
  void userIsPastSubmissionGateWhenNoSuccessfulSubmissionExists() {
    assertThat(planningService.isUserPastSubmissionGate(user, currentTime), is(true));
  }

  @Nested
  class WithLastSuccessfulSubmission {
    Timestamp lastSuccessfulSubmission;

    @BeforeEach
    void setup() {
      lastSuccessfulSubmission = makeMe.aTimestamp().of(9, 8).fromShanghai().please();
      QuestionGenerationBatchUserState state = new QuestionGenerationBatchUserState();
      state.setUser(user);
      state.setLastSuccessfulSubmittedAt(lastSuccessfulSubmission);
      userStateRepository.save(state);
      makeMe.entityPersister.flush();
    }

    @Test
    void userIsNotPastSubmissionGateWhenLastSubmissionIs22Hours59MinutesOld() {
      Timestamp at =
          new Timestamp(
              lastSuccessfulSubmission.getTime()
                  + TimeUnit.HOURS.toMillis(22)
                  + TimeUnit.MINUTES.toMillis(59));

      assertThat(planningService.isUserPastSubmissionGate(user, at), is(false));
    }

    @Test
    void userIsPastSubmissionGateWhenLastSubmissionIsExactly23HoursOld() {
      Timestamp at =
          new Timestamp(lastSuccessfulSubmission.getTime() + TimeUnit.HOURS.toMillis(23));

      assertThat(planningService.isUserPastSubmissionGate(user, at), is(true));
    }

    @Test
    void userIsPastSubmissionGateWhenLastSubmissionIsJustOver23HoursOld() {
      Timestamp at =
          new Timestamp(
              lastSuccessfulSubmission.getTime()
                  + TimeUnit.HOURS.toMillis(23)
                  + TimeUnit.MINUTES.toMillis(1));

      assertThat(planningService.isUserPastSubmissionGate(user, at), is(true));
    }
  }
}
