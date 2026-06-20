package com.odde.doughnut.entities.repositories;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
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
class QuestionGenerationBatchLatestSubmittedAtRepositoryTest {

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchRepository repository;

  User user;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
  }

  @Test
  void returnsEmptyWhenUserHasNoSubmittedBatch() {
    Optional<Timestamp> latest = repository.findLatestSubmittedAtByUser_Id(user.getId());
    assertThat(latest.isPresent(), is(false));
  }

  @Nested
  class LatestSubmittedAt {
    Timestamp firstSubmission;
    Timestamp updatedSubmission;

    @BeforeEach
    void setup() {
      firstSubmission = makeMe.aTimestamp().of(10, 8).fromShanghai().please();
      updatedSubmission = makeMe.aTimestamp().of(15, 8).fromShanghai().please();
    }

    @Test
    void returnsSubmittedAtFromBatch() {
      saveSubmittedBatchAt(firstSubmission);

      Timestamp latest = repository.findLatestSubmittedAtByUser_Id(user.getId()).orElseThrow();

      assertThat(latest, equalTo(firstSubmission));
    }

    @Test
    void returnsMaxSubmittedAtAcrossBatches() {
      saveSubmittedBatchAt(firstSubmission);
      saveSubmittedBatchAt(updatedSubmission);

      Timestamp latest = repository.findLatestSubmittedAtByUser_Id(user.getId()).orElseThrow();

      assertThat(latest, equalTo(updatedSubmission));
    }

    @Test
    void ignoresBatchesWithoutSubmittedAt() {
      QuestionGenerationBatch failedBatch = new QuestionGenerationBatch();
      failedBatch.setUser(user);
      failedBatch.setStatus(QuestionGenerationBatchStatus.FAILED);
      failedBatch.setPlannedAt(firstSubmission);
      repository.save(failedBatch);
      makeMe.entityPersister.flush();

      Optional<Timestamp> latest = repository.findLatestSubmittedAtByUser_Id(user.getId());
      assertThat(latest.isPresent(), is(false));
    }
  }

  private void saveSubmittedBatchAt(Timestamp submittedAt) {
    QuestionGenerationBatch batch = new QuestionGenerationBatch();
    batch.setUser(user);
    batch.setStatus(QuestionGenerationBatchStatus.COMPLETED);
    batch.setPlannedAt(submittedAt);
    batch.setSubmittedAt(submittedAt);
    repository.save(batch);
    makeMe.entityPersister.flush();
  }
}
