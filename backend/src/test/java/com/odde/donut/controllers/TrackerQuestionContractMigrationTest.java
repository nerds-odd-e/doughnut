package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.testability.SpringTestBase;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TrackerQuestionContractMigrationTest extends SpringTestBase {
  @Autowired Flyway applicationFlyway;
  @Autowired EntityManager entityManager;
  @Autowired PlatformTransactionManager transactionManager;

  @Test
  void pendingQuestionCleanupKeepsTheUpgradeableSchemaBeforeAlter() throws Exception {
    try (var upgrade =
        new TrackerQuestionUpgradeFixture(applicationFlyway, entityManager, transactionManager)) {
      upgrade.migrations("300000359").migrate();
      Integer questionId =
          upgrade.withTargetCatalog(
              () -> makeMe.anMcq().stem("Pending cleanup").please().getId(),
              id -> {
                Number count =
                    (Number)
                        entityManager
                            .createNativeQuery(
                                "SELECT COUNT(*) FROM `" + upgrade.catalog + "`.mcq WHERE id=:id")
                            .setParameter("id", id)
                            .getSingleResult();
                assertThat(count.intValue(), equalTo(1));
              });

      FlywayException refusal =
          assertThrows(FlywayException.class, () -> upgrade.migrations("300000360").migrate());

      assertThat(
          NestedExceptionUtils.getMostSpecificCause(refusal).getMessage(),
          containsString("requires unreferenced question cleanup"));
      assertThat(upgrade.questionIds(List.of(questionId)), equalTo(List.of(questionId)));
      assertThat(
          upgrade.jdbc.queryForObject(
              "SELECT IS_NULLABLE FROM information_schema.columns WHERE TABLE_SCHEMA=DATABASE() "
                  + "AND TABLE_NAME='mcq' AND COLUMN_NAME='note_id'",
              String.class),
          equalTo("YES"));
      assertThat(
          upgrade.jdbc.queryForList(
              "SELECT COLUMN_NAME FROM information_schema.statistics WHERE TABLE_SCHEMA=DATABASE() "
                  + "AND TABLE_NAME='mcq' AND INDEX_NAME='fk_mcq_note' ORDER BY SEQ_IN_INDEX",
              String.class),
          equalTo(List.of("note_id")));
    }
  }
}
