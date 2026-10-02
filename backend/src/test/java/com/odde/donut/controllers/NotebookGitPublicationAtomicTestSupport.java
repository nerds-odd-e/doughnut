package com.odde.donut.controllers;

import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.factoryServices.EntityPersister;
import jakarta.persistence.EntityManager;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

final class NotebookGitPublicationAtomicTestSupport {

  static final AtomicBoolean FAIL_ON_BINDING_SAVE = new AtomicBoolean(false);
  static final AtomicReference<Integer> FAIL_FOR_NOTEBOOK_ID = new AtomicReference<>();

  private NotebookGitPublicationAtomicTestSupport() {}

  @TestConfiguration
  @Profile("notebook-git-publication-atomic-test")
  static class FailingBindingSaveConfig {
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
      if (FAIL_ON_BINDING_SAVE.get()
          && entity instanceof NotebookGitBinding binding
          && (FAIL_FOR_NOTEBOOK_ID.get() == null
              || FAIL_FOR_NOTEBOOK_ID.get().equals(binding.getNotebook().getId()))) {
        throw new RuntimeException("forced failure after note projection");
      }
      return super.save(entity);
    }
  }
}
