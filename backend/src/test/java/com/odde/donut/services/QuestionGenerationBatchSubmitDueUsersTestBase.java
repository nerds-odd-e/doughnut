package com.odde.donut.services;

import static org.mockito.Mockito.reset;

import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.donut.services.openAiApis.OpenAiApiHandler;
import com.odde.donut.testability.CommittedUserCleanup;
import com.odde.donut.testability.MakeMe;
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

  static final String COMMITTED_USER_PREFIX = "batch-due-";

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
    String identifier = COMMITTED_USER_PREFIX + UUID.randomUUID();
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

  @BeforeEach
  void cleanupStaleCommittedFixtures() {
    inCommittedTransaction(this::deleteCommittedDueUserFixtures);
  }

  @AfterEach
  void cleanupCommittedState() {
    reset(planningService);
    inCommittedTransaction(this::deleteCommittedDueUserFixtures);
  }

  private void deleteCommittedDueUserFixtures() {
    CommittedUserCleanup.deleteByUserExternalIdentifierLike(
        entityManager, COMMITTED_USER_PREFIX + "%");
  }
}
