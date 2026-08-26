package com.odde.donut.services;

import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.factoryServices.EntityPersister;
import jakarta.persistence.EntityManager;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

final class QuestionGenerationBatchRowImportAtomicTestSupport {

  static final AtomicBoolean FAIL_ON_RECALL_PROMPT_SAVE = new AtomicBoolean(false);

  private QuestionGenerationBatchRowImportAtomicTestSupport() {}

  static void deleteCommittedAtomicImportFixtures(EntityManager entityManager) {
    entityManager
        .createNativeQuery(
            "DELETE rp FROM recall_prompt rp "
                + "INNER JOIN memory_tracker mt ON rp.memory_tracker_id = mt.id "
                + "INNER JOIN user u ON mt.user_id = u.id "
                + "WHERE u.external_identifier LIKE 'batch-import-atomic-%'")
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE mcq FROM mcq "
                + "INNER JOIN note n ON mcq.note_id = n.id "
                + "INNER JOIN notebook nb ON n.notebook_id = nb.id "
                + "INNER JOIN ownership o ON nb.ownership_id = o.id "
                + "INNER JOIN user u ON o.user_id = u.id "
                + "WHERE u.external_identifier LIKE 'batch-import-atomic-%'")
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE qgr FROM question_generation_batch_request qgr "
                + "INNER JOIN question_generation_batch qgb ON qgr.batch_id = qgb.id "
                + "INNER JOIN user u ON qgb.user_id = u.id "
                + "WHERE u.external_identifier LIKE 'batch-import-atomic-%'")
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE qgb FROM question_generation_batch qgb "
                + "INNER JOIN user u ON qgb.user_id = u.id "
                + "WHERE u.external_identifier LIKE 'batch-import-atomic-%'")
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE mt FROM memory_tracker mt "
                + "INNER JOIN user u ON mt.user_id = u.id "
                + "WHERE u.external_identifier LIKE 'batch-import-atomic-%'")
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE n FROM note n "
                + "INNER JOIN notebook nb ON n.notebook_id = nb.id "
                + "INNER JOIN ownership o ON nb.ownership_id = o.id "
                + "INNER JOIN user u ON o.user_id = u.id "
                + "WHERE u.external_identifier LIKE 'batch-import-atomic-%'")
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE nb FROM notebook nb "
                + "INNER JOIN ownership o ON nb.ownership_id = o.id "
                + "INNER JOIN user u ON o.user_id = u.id "
                + "WHERE u.external_identifier LIKE 'batch-import-atomic-%'")
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE o FROM ownership o "
                + "INNER JOIN user u ON o.user_id = u.id "
                + "WHERE u.external_identifier LIKE 'batch-import-atomic-%'")
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE FROM user WHERE external_identifier LIKE 'batch-import-atomic-%'")
        .executeUpdate();
  }

  @TestConfiguration
  @Profile("batch-row-import-atomic-test")
  static class FailingRecallPromptImportConfig {
    @Bean
    @Primary
    EntityPersister entityPersister(EntityManager entityManager) {
      return new FailableEntityPersister(entityManager);
    }
  }

  static class FailableEntityPersister extends EntityPersister {
    FailableEntityPersister(EntityManager entityManager) {
      super(entityManager);
    }

    @Override
    public <T> T save(T entity) {
      if (FAIL_ON_RECALL_PROMPT_SAVE.get() && entity instanceof RecallPrompt) {
        throw new RuntimeException("forced failure after question creation");
      }
      return super.save(entity);
    }
  }
}
