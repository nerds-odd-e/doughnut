package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.testability.SpringTestBase;
import jakarta.persistence.EntityManager;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TrackerQuestionUpgradeIsolationTest extends SpringTestBase {
  @Autowired Flyway applicationFlyway;
  @Autowired EntityManager entityManager;
  @Autowired PlatformTransactionManager transactionManager;
  @Autowired JdbcTemplate sharedJdbc;

  @Test
  void canonicalSchemaReplayRetainsAskedEvidenceInItsOwnedCatalog() throws Exception {
    String sourceCatalog = sharedJdbc.queryForObject("SELECT DATABASE()", String.class);
    int sentinel =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .anMcq()
                    .stem("Original catalog evidence")
                    .choices("alternative", "source key")
                    .correctAnswerIndex(1)
                    .please()
                    .getId());
    try {
      var sentinelPayload = sharedJdbc.queryForMap("SELECT * FROM mcq WHERE id = ?", sentinel);
      assertThat(
          sentinelPayload.get("raw_json_question").toString(),
          containsString("Original catalog evidence"));
      assertThat(((Number) sentinelPayload.get("correct_answer_index")).intValue(), equalTo(1));
      var sharedQuestions = sharedJdbc.queryForList("SELECT id FROM mcq ORDER BY id");
      var sharedHistory =
          sharedJdbc.queryForList("SELECT * FROM flyway_schema_history ORDER BY installed_rank");
      try (var upgrade =
          new TrackerQuestionUpgradeFixture(applicationFlyway, entityManager, transactionManager)) {
        Map<String, Integer> graph =
            upgrade.withTargetCatalog(
                () -> {
                  var user = makeMe.aUser("upgrade-learner").please();
                  var note =
                      makeMe.aNote().notebookOwnedBy(user).title("Learner evidence").please();
                  var tracker = makeMe.aMemoryTrackerFor(note).by(user).please();
                  var question =
                      makeMe
                          .anMcq()
                          .stem("Which evidence?")
                          .choices("key", "alternative", "learner selection")
                          .correctAnswerIndex(0)
                          .please();
                  var prompt =
                      makeMe.aRecallPrompt().forMemoryTracker(tracker).withMcq(question).please();
                  return Map.of(
                      "user",
                      user.getId(),
                      "note",
                      note.getId(),
                      "memory_tracker",
                      tracker.getId(),
                      "mcq",
                      question.getId(),
                      "recall_prompt",
                      prompt.getId());
                },
                ids -> {
                  Number matched =
                      (Number)
                          entityManager
                              .createNativeQuery(
                                  "SELECT COUNT(*) FROM `"
                                      + upgrade.catalog
                                      + "`.recall_prompt p "
                                      + "JOIN `"
                                      + upgrade.catalog
                                      + "`.mcq q ON q.id=p.mcq_id "
                                      + "JOIN `"
                                      + upgrade.catalog
                                      + "`.memory_tracker t ON t.id=p.memory_tracker_id "
                                      + "JOIN `"
                                      + upgrade.catalog
                                      + "`.note n ON n.id=t.note_id "
                                      + "JOIN `"
                                      + upgrade.catalog
                                      + "`.user u ON u.id=t.user_id "
                                      + "WHERE p.id=:prompt AND q.id=:question AND t.id=:tracker AND n.id=:note AND u.id=:user")
                              .setParameter("prompt", ids.get("recall_prompt"))
                              .setParameter("question", ids.get("mcq"))
                              .setParameter("tracker", ids.get("memory_tracker"))
                              .setParameter("note", ids.get("note"))
                              .setParameter("user", ids.get("user"))
                              .getSingleResult();
                  assertThat(matched.intValue(), equalTo(1));
                });
        var before = upgrade.snapshot(graph);
        assertThat(
            before.get("mcq").getFirst().get("raw_json_question").toString(),
            containsString("Which evidence?"));
        assertThat(
            ((Number) before.get("recall_prompt").getFirst().get("mcq_id")).intValue(),
            equalTo(graph.get("mcq")));

        upgrade.migrations("300000357").migrate();

        assertThat(upgrade.snapshot(graph), equalTo(before));
      }
      assertThat(
          sharedJdbc.queryForList("SELECT id FROM mcq ORDER BY id"), equalTo(sharedQuestions));
      assertThat(
          sharedJdbc.queryForList("SELECT * FROM flyway_schema_history ORDER BY installed_rank"),
          equalTo(sharedHistory));
      assertThat(
          sharedJdbc.queryForMap("SELECT * FROM mcq WHERE id = ?", sentinel),
          equalTo(sentinelPayload));
    } finally {
      inCommittedTransaction(
          transactionManager,
          () -> {
            sharedJdbc.update("DELETE FROM `" + sourceCatalog + "`.mcq WHERE id = ?", sentinel);
            entityManager.clear();
          });
    }
  }
}
