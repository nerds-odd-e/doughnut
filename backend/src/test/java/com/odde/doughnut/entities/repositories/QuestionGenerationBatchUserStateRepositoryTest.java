package com.odde.doughnut.entities.repositories;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.doughnut.entities.QuestionGenerationBatchUserState;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.util.Optional;
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
class QuestionGenerationBatchUserStateRepositoryTest {

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchUserStateRepository repository;

  User user;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
  }

  @Test
  void returnsEmptyWhenUserHasNoSuccessfulSubmission() {
    Optional<QuestionGenerationBatchUserState> state = repository.findByUser_Id(user.getId());
    assertThat(state.isPresent(), is(false));
  }

  @Nested
  class RecordLastSuccessfulSubmission {
    Timestamp firstSubmission;
    Timestamp updatedSubmission;

    @BeforeEach
    void setup() {
      firstSubmission = makeMe.aTimestamp().of(10, 8).fromShanghai().please();
      updatedSubmission = makeMe.aTimestamp().of(15, 8).fromShanghai().please();
    }

    @Test
    void persistsAndReadsLastSuccessfulSubmittedAt() {
      QuestionGenerationBatchUserState state = new QuestionGenerationBatchUserState();
      state.setUser(user);
      state.setLastSuccessfulSubmittedAt(firstSubmission);
      repository.save(state);
      makeMe.entityPersister.flush();

      QuestionGenerationBatchUserState loaded =
          repository.findByUser_Id(user.getId()).orElseThrow();

      assertThat(loaded.getLastSuccessfulSubmittedAt(), equalTo(firstSubmission));
    }

    @Test
    void updatesLastSuccessfulSubmittedAt() {
      QuestionGenerationBatchUserState state = new QuestionGenerationBatchUserState();
      state.setUser(user);
      state.setLastSuccessfulSubmittedAt(firstSubmission);
      repository.save(state);
      makeMe.entityPersister.flush();

      state.setLastSuccessfulSubmittedAt(updatedSubmission);
      repository.save(state);
      makeMe.entityPersister.flush();

      QuestionGenerationBatchUserState loaded =
          repository.findByUser_Id(user.getId()).orElseThrow();

      assertThat(loaded.getLastSuccessfulSubmittedAt(), equalTo(updatedSubmission));
    }
  }
}
