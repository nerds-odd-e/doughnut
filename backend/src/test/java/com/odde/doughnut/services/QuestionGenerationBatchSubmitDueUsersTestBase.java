package com.odde.doughnut.services;

import static org.mockito.Mockito.reset;

import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.MakeMe;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class QuestionGenerationBatchSubmitDueUsersTestBase {

  @MockitoBean OpenAiApiHandler openAiApiHandler;

  @Autowired MakeMe makeMe;
  @MockitoSpyBean QuestionGenerationBatchPlanningService planningService;
  @Autowired QuestionGenerationBatchSubmitDueUsersService submitDueUsersService;
  @Autowired QuestionGenerationBatchRepository batchRepository;
  @Autowired EntityManager entityManager;
  @Autowired PlatformTransactionManager transactionManager;

  final Timestamp cronTime = Timestamp.valueOf(LocalDateTime.of(2024, 8, 3, 16, 45));

  User uniqueUser() {
    User user = new User();
    String identifier = "batch-due-" + UUID.randomUUID();
    user.setExternalIdentifier(identifier);
    user.setName(identifier);
    makeMe.entityPersister.save(user);
    return user;
  }

  void inCommittedTransaction(Runnable action) {
    TransactionTemplate template = new TransactionTemplate(transactionManager);
    template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    template.executeWithoutResult(status -> action.run());
  }

  void deleteCommittedBatchWorkForBatchDueUsers() {
    entityManager
        .createNativeQuery(
            "DELETE qgr FROM question_generation_batch_request qgr "
                + "INNER JOIN question_generation_batch qgb ON qgr.batch_id = qgb.id "
                + "INNER JOIN user u ON qgb.user_id = u.id "
                + "WHERE u.external_identifier LIKE 'batch-due-%'")
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE qgb FROM question_generation_batch qgb "
                + "INNER JOIN user u ON qgb.user_id = u.id "
                + "WHERE u.external_identifier LIKE 'batch-due-%'")
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE rp FROM recall_prompt rp "
                + "INNER JOIN memory_tracker mt ON rp.memory_tracker_id = mt.id "
                + "INNER JOIN user u ON mt.user_id = u.id "
                + "WHERE u.external_identifier LIKE 'batch-due-%'")
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE mt FROM memory_tracker mt "
                + "INNER JOIN user u ON mt.user_id = u.id "
                + "WHERE u.external_identifier LIKE 'batch-due-%'")
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE n FROM note n "
                + "INNER JOIN notebook nb ON n.notebook_id = nb.id "
                + "INNER JOIN user u ON nb.creator_id = u.id "
                + "WHERE u.external_identifier LIKE 'batch-due-%'")
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "DELETE nb FROM notebook nb "
                + "INNER JOIN user u ON nb.creator_id = u.id "
                + "WHERE u.external_identifier LIKE 'batch-due-%'")
        .executeUpdate();
    entityManager
        .createNativeQuery("DELETE FROM user WHERE external_identifier LIKE 'batch-due-%'")
        .executeUpdate();
  }

  @BeforeEach
  void cleanupStaleCommittedFixtures() {
    inCommittedTransaction(this::deleteCommittedBatchWorkForBatchDueUsers);
  }

  @AfterEach
  void cleanupCommittedState() {
    reset(planningService);
    inCommittedTransaction(this::deleteCommittedBatchWorkForBatchDueUsers);
  }
}
