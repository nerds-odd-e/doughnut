package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.testability.SpringTestBase;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TrackerQuestionCleanupMigrationTest extends SpringTestBase {
  @Autowired Flyway applicationFlyway;
  @Autowired EntityManager entityManager;
  @Autowired PlatformTransactionManager transactionManager;

  @Test
  void defaultCleanupGateKeepsPopulatedQuestionsAndTheirAskedEvidence() throws Exception {
    assertThat(
        applicationFlyway.getConfiguration().getPlaceholders().get("questioncleanup"),
        equalTo("1=0"));
    try (var upgrade =
        new TrackerQuestionUpgradeFixture(applicationFlyway, entityManager, transactionManager)) {
      upgrade.migrations("300000358").migrate();
      Graph graph = populate(upgrade);
      var before = upgrade.snapshot(graph.rows());
      var orphanPayload = upgrade.snapshot(Map.of("mcq", graph.orphans()));

      upgrade.migrations("300000359").migrate();

      assertThat(upgrade.questionIds(graph.questions()), equalTo(graph.questions()));
      assertThat(upgrade.snapshot(graph.rows()), equalTo(before));
      assertThat(upgrade.snapshot(Map.of("mcq", graph.orphans())), equalTo(orphanPayload));
    }
  }

  @Test
  void deliberateRetryCleansOnlyPromptlessQuestionsAndPreservesEveryAskedRow() throws Exception {
    try (var upgrade =
        new TrackerQuestionUpgradeFixture(applicationFlyway, entityManager, transactionManager)) {
      Graph graph = populate(upgrade);
      var before = upgrade.snapshot(graph.rows());
      Flyway guarded = upgrade.migrations("300000359");

      FlywayException refusal = assertThrows(FlywayException.class, guarded::migrate);

      assertThat(
          NestedExceptionUtils.getMostSpecificCause(refusal).getMessage(),
          containsString("requires deliberate enablement"));
      assertThat(upgrade.questionIds(graph.questions()), equalTo(graph.questions()));
      assertThat(upgrade.snapshot(graph.rows()), equalTo(before));
      Flyway enabled = upgrade.migrations("300000359", Map.of("questioncleanup", "1=1"));
      enabled.repair();
      enabled.migrate();

      assertThat(upgrade.questionIds(graph.questions()), equalTo(graph.askedQuestions()));
      assertThat(upgrade.snapshot(graph.rows()), equalTo(before));
    }
  }

  private Graph populate(TrackerQuestionUpgradeFixture upgrade) {
    Graph graph =
        upgrade.withTargetCatalog(
            this::createGraph,
            graphIds -> {
              graphIds
                  .rows()
                  .forEach((table, ids) -> {
                    Number count =
                        (Number)
                            entityManager
                                .createNativeQuery(
                                    "SELECT COUNT(*) FROM `"
                                        + upgrade.catalog
                                        + "`."
                                        + table
                                        + " WHERE id IN (:ids)")
                                .setParameter("ids", ids)
                                .getSingleResult();
                    assertThat(count.intValue(), equalTo(ids.size()));
                  });
              Number count =
                  (Number)
                      entityManager
                          .createNativeQuery(
                              "SELECT COUNT(*) FROM `"
                                  + upgrade.catalog
                                  + "`.mcq WHERE id IN (:ids)")
                          .setParameter("ids", graphIds.orphans())
                          .getSingleResult();
              assertThat(count.intValue(), equalTo(2));
              String prefix = "`" + upgrade.catalog + "`.";
              Number joined =
                  (Number)
                      entityManager
                          .createNativeQuery(
                              "SELECT COUNT(*) FROM "
                                  + prefix
                                  + "recall_prompt p JOIN "
                                  + prefix
                                  + "mcq q ON q.id=p.mcq_id JOIN "
                                  + prefix
                                  + "memory_tracker t ON t.id=p.memory_tracker_id JOIN "
                                  + prefix
                                  + "note n ON n.id=t.note_id JOIN "
                                  + prefix
                                  + "user u ON u.id=t.user_id JOIN "
                                  + prefix
                                  + "answer a ON a.id=p.answer_id JOIN "
                                  + prefix
                                  + "recall_log l ON l.answer_id=a.id AND l.memory_tracker_id=t.id JOIN "
                                  + prefix
                                  + "conversation c ON c.recall_prompt_id=p.id JOIN "
                                  + prefix
                                  + "conversation_message m ON m.conversation_id=c.id JOIN "
                                  + prefix
                                  + "conversation_note_correction x ON x.source_message_id=m.id AND x.note_id=n.id "
                                  + "WHERE p.id=:prompt AND u.id=:owner")
                          .setParameter("prompt", graphIds.rows().get("recall_prompt").getFirst())
                          .setParameter("owner", graphIds.rows().get("user").getFirst())
                          .getSingleResult();
              assertThat(joined.intValue(), equalTo(1));
            });
    assertThat(upgrade.cleanupCandidates(), equalTo(graph.orphans()));
    var rows = upgrade.snapshot(graph.rows());
    assertThat(
        rows.get("mcq").getFirst().get("raw_json_question").toString(),
        containsString("Which evidence?"));
    assertThat(
        ((Number) rows.get("mcq").getFirst().get("correct_answer_index")).intValue(), equalTo(0));
    var answer = rows.get("answer").getFirst();
    assertThat(((Number) answer.get("choice_index")).intValue(), equalTo(2));
    assertThat(answer.get("exclusion_reason"), equalTo("two interpretations"));
    assertThat(answer.get("excluded_at"), equalTo(Timestamp.valueOf("2026-11-11 08:00:00")));
    assertThat(answer.get("created_at"), equalTo(Timestamp.valueOf("2026-11-11 08:00:00")));
    assertThat(rows.get("recall_log").getFirst().get("product_outcome"), equalTo("AGAIN"));
    var prompts = rows.get("recall_prompt");
    assertThat(
        prompts.getFirst().get("created_at"), equalTo(Timestamp.valueOf("2026-11-10 08:00:00")));
    assertThat(prompts.get(1).get("mcq_id"), equalTo(prompts.getFirst().get("mcq_id")));
    assertThat(prompts.get(2).get("answer_id"), nullValue());
    assertThat(
        rows.get("conversation_message").getFirst().get("message"),
        equalTo("Preserve the learner evidence"));
    assertThat(
        rows.get("conversation_note_correction").getFirst().get("decision"), equalTo("ACCEPTED"));
    return graph;
  }

  private Graph createGraph() {
    var user = makeMe.aUser("cleanup-learner").please();
    var note = makeMe.aNote().notebookOwnedBy(user).title("Evidence").please();
    var tracker = makeMe.aMemoryTrackerFor(note).by(user).please();
    var question =
        makeMe
            .anMcq()
            .stem("Which evidence?")
            .choices("key", "alternative", "learner selection")
            .correctAnswerIndex(0)
            .please();
    var asked =
        makeMe
            .aRecallPrompt()
            .forMemoryTracker(tracker)
            .withMcq(question)
            .questionGeneratedTime(Timestamp.valueOf("2026-11-10 08:00:00"))
            .answerTimestamp(Timestamp.valueOf("2026-11-11 08:00:00"))
            .answerChoiceIndex(2)
            .recorded()
            .excludedAsAmbiguous("two interpretations")
            .please();
    var shared = makeMe.aRecallPrompt().forMemoryTracker(tracker).withMcq(question).please();
    var unanswered = makeMe.aRecallPrompt().forMemoryTracker(tracker).please();
    var conversation = makeMe.aConversation().forARecallPrompt(asked).from(user).please();
    var message =
        makeMe
            .aConversationMessage(conversation)
            .sender(user)
            .message("Preserve the learner evidence")
            .please();
    var correction = makeMe.aConversationNoteCorrection(message).acceptedBy(user).please();
    List<Integer> orphans =
        List.of(
            makeMe.anMcq().stem("Unreferenced candidate one").please().getId(),
            makeMe.anMcq().stem("Unreferenced candidate two").please().getId());
    Map<String, List<Integer>> rows = new LinkedHashMap<>();
    rows.put("user", List.of(user.getId()));
    rows.put("note", List.of(note.getId()));
    rows.put("memory_tracker", List.of(tracker.getId()));
    rows.put("mcq", List.of(question.getId(), unanswered.getMcq().getId()));
    rows.put("recall_prompt", List.of(asked.getId(), shared.getId(), unanswered.getId()));
    rows.put("answer", List.of(asked.getAnswer().getId()));
    rows.put("recall_log", List.of(asked.getAnswer().getRecallLogs().getFirst().getId()));
    rows.put("conversation", List.of(conversation.getId()));
    rows.put("conversation_message", List.of(message.getId()));
    rows.put("conversation_note_correction", List.of(correction.getId()));
    List<Integer> allQuestions = new ArrayList<>(rows.get("mcq"));
    allQuestions.addAll(orphans);
    allQuestions.sort(Integer::compareTo);
    return new Graph(rows, List.copyOf(allQuestions), rows.get("mcq"), orphans);
  }

  private record Graph(
      Map<String, List<Integer>> rows,
      List<Integer> questions,
      List<Integer> askedQuestions,
      List<Integer> orphans) {}
}
