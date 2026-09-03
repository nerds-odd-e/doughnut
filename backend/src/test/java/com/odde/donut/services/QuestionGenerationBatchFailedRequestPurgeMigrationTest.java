package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.QuestionGenerationBatch;
import com.odde.donut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.donut.entities.QuestionGenerationBatchStatus;
import com.odde.donut.entities.User;
import com.odde.donut.testability.MakeMe;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionGenerationBatchFailedRequestPurgeMigrationTest {

  private static final String PURGE_MIGRATION =
      "db/migration/V300000318__purge_failed_question_generation_batch_requests.sql";
  private static final String PURGE_GATE_PLACEHOLDER =
      "${question_generation_batch_failed_request_purge}";

  @Autowired MakeMe makeMe;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired QuestionGenerationBatchPlanningService planningService;

  User user;
  Timestamp currentTime;
  MemoryTracker twiceFailedTracker;
  QuestionGenerationBatch firstFailedBatch;
  QuestionGenerationBatch secondFailedBatch;
  QuestionGenerationBatch pendingBatch;
  QuestionGenerationBatch outputReadyBatch;
  QuestionGenerationBatch importedBatch;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    currentTime = makeMe.aTimestamp().of(10, 8).fromShanghai().please();

    Note dueNote = makeMe.aNote().notebookOwnedBy(user).please();
    twiceFailedTracker =
        makeMe
            .aMemoryTrackerFor(dueNote)
            .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(24)))
            .please();

    firstFailedBatch =
        makeMe
            .aQuestionGenerationBatch()
            .forUser(user)
            .status(QuestionGenerationBatchStatus.FAILED)
            .plannedAt(currentTime)
            .please();
    makeMe
        .aQuestionGenerationBatchRequest()
        .batch(firstFailedBatch)
        .memoryTracker(twiceFailedTracker)
        .status(QuestionGenerationBatchRequestStatus.FAILED)
        .please();

    secondFailedBatch =
        makeMe
            .aQuestionGenerationBatch()
            .forUser(user)
            .status(QuestionGenerationBatchStatus.FAILED)
            .plannedAt(currentTime)
            .please();
    makeMe
        .aQuestionGenerationBatchRequest()
        .batch(secondFailedBatch)
        .memoryTracker(twiceFailedTracker)
        .status(QuestionGenerationBatchRequestStatus.FAILED)
        .please();

    Note otherNote = makeMe.aNote().notebookOwnedBy(user).please();
    MemoryTracker otherTracker = makeMe.aMemoryTrackerFor(otherNote).please();

    pendingBatch =
        makeMe
            .aQuestionGenerationBatch()
            .forUser(user)
            .status(QuestionGenerationBatchStatus.SUBMITTED)
            .plannedAt(currentTime)
            .please();
    makeMe
        .aQuestionGenerationBatchRequest()
        .batch(pendingBatch)
        .memoryTracker(otherTracker)
        .status(QuestionGenerationBatchRequestStatus.PENDING)
        .please();

    outputReadyBatch =
        makeMe
            .aQuestionGenerationBatch()
            .forUser(user)
            .status(QuestionGenerationBatchStatus.COMPLETED)
            .plannedAt(currentTime)
            .please();
    makeMe
        .aQuestionGenerationBatchRequest()
        .batch(outputReadyBatch)
        .memoryTracker(otherTracker)
        .status(QuestionGenerationBatchRequestStatus.OUTPUT_READY)
        .please();

    importedBatch =
        makeMe
            .aQuestionGenerationBatch()
            .forUser(user)
            .status(QuestionGenerationBatchStatus.COMPLETED)
            .plannedAt(currentTime)
            .completedAt(currentTime)
            .importedAt(currentTime)
            .please();
    makeMe
        .aQuestionGenerationBatchRequest()
        .batch(importedBatch)
        .memoryTracker(otherTracker)
        .status(QuestionGenerationBatchRequestStatus.IMPORTED)
        .please();

    makeMe.entityPersister.flush();
  }

  @Test
  void defaultGateLeavesFailedRequestsInPlace() {
    jdbcTemplate.update(purgeSql("1=0"));

    assertThat(requestCountFor(firstFailedBatch.getId()), is(1L));
    assertThat(requestCountFor(secondFailedBatch.getId()), is(1L));
    assertThat(requestCountFor(pendingBatch.getId()), is(1L));
    assertThat(requestCountFor(outputReadyBatch.getId()), is(1L));
    assertThat(requestCountFor(importedBatch.getId()), is(1L));
  }

  @Test
  void enabledGateDeletesOnlyFailedRequestsAndUnblocksCandidateTracker() {
    jdbcTemplate.update(purgeSql("1=1"));

    assertThat(requestCountFor(firstFailedBatch.getId()), is(0L));
    assertThat(requestCountFor(secondFailedBatch.getId()), is(0L));
    assertThat(requestCountFor(pendingBatch.getId()), is(1L));
    assertThat(requestCountFor(outputReadyBatch.getId()), is(1L));
    assertThat(requestCountFor(importedBatch.getId()), is(1L));

    assertThat(batchExists(firstFailedBatch.getId()), is(true));
    assertThat(batchExists(secondFailedBatch.getId()), is(true));

    List<MemoryTracker> candidates =
        planningService.findCandidateMemoryTrackersForBatchGeneration(user, currentTime);
    assertThat(
        candidates.stream().map(MemoryTracker::getId).toList(),
        contains(twiceFailedTracker.getId()));
  }

  private static String purgeSql(String gate) {
    try (InputStream in =
        QuestionGenerationBatchFailedRequestPurgeMigrationTest.class
            .getClassLoader()
            .getResourceAsStream(PURGE_MIGRATION)) {
      if (in == null) {
        throw new IllegalStateException("Missing classpath resource " + PURGE_MIGRATION);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8)
          .replace(PURGE_GATE_PLACEHOLDER, gate);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read " + PURGE_MIGRATION, e);
    }
  }

  private boolean batchExists(Integer batchId) {
    Long count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM question_generation_batch WHERE id = ?", Long.class, batchId);
    return count != null && count == 1L;
  }

  private long requestCountFor(Integer batchId) {
    Long count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM question_generation_batch_request WHERE batch_id = ?",
            Long.class,
            batchId);
    return count == null ? 0L : count;
  }
}
