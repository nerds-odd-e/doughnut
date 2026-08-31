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
