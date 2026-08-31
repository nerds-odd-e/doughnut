package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
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
class QuestionGenerationBatchIncompletePurgeMigrationTest {

  private static final String PURGE_MIGRATION =
      "db/migration/V300000306__purge_incomplete_question_generation_batches.sql";
  private static final String PURGE_GATE_PLACEHOLDER =
      "${question_generation_batch_incomplete_purge}";

  @Autowired MakeMe makeMe;
  @Autowired JdbcTemplate jdbcTemplate;

  User user;
  MemoryTracker memoryTracker;
  Timestamp currentTime;
  QuestionGenerationBatch importedBatch;
  QuestionGenerationBatch failedBatch;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    Note note = makeMe.aNote().notebookOwnedBy(user).please();
    memoryTracker = makeMe.aMemoryTrackerFor(note).please();
    currentTime = makeMe.aTimestamp().please();

    importedBatch =
        makeMe
            .aQuestionGenerationBatch()
            .forUser(user)
            .completedAt(currentTime)
            .importedAt(currentTime)
            .please();
    makeMe
        .aQuestionGenerationBatchRequest()
        .batch(importedBatch)
        .memoryTracker(memoryTracker)
        .status(QuestionGenerationBatchRequestStatus.IMPORTED)
        .please();

    failedBatch =
        makeMe
            .aQuestionGenerationBatch()
            .forUser(user)
            .status(QuestionGenerationBatchStatus.FAILED)
            .plannedAt(currentTime)
            .openaiBatchId("batch-failed")
            .please();
    makeMe
        .aQuestionGenerationBatchRequest()
        .batch(failedBatch)
        .memoryTracker(memoryTracker)
        .status(QuestionGenerationBatchRequestStatus.PENDING)
        .please();
    makeMe.entityPersister.flush();
  }

  @Test
  void defaultGateLeavesIncompleteBatchesInPlace() {
    jdbcTemplate.update(purgeSql("1=0"));

    assertThat(batchExists(importedBatch.getId()), is(true));
    assertThat(batchExists(failedBatch.getId()), is(true));
    assertThat(requestCountFor(importedBatch.getId()), is(1L));
    assertThat(requestCountFor(failedBatch.getId()), is(1L));
  }

  @Test
  void enabledGateDeletesIncompleteBatchesAndKeepsImportedCompletedBatches() {
    jdbcTemplate.update(purgeSql("1=1"));

    assertThat(batchExists(importedBatch.getId()), is(true));
    assertThat(batchExists(failedBatch.getId()), is(false));
    assertThat(requestCountFor(importedBatch.getId()), is(1L));
    assertThat(requestCountFor(failedBatch.getId()), is(0L));
  }

  private static String purgeSql(String gate) {
    try (InputStream in =
        QuestionGenerationBatchIncompletePurgeMigrationTest.class
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
