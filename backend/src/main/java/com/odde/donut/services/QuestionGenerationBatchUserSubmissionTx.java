package com.odde.donut.services;

import com.odde.donut.entities.QuestionGenerationBatch;
import com.odde.donut.entities.User;
import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionGenerationBatchUserSubmissionTx {
  private final QuestionGenerationBatchPlanningService planningService;
  private final QuestionGenerationBatchSubmissionService submissionService;
  private final QuestionGenerationBatchUserSubmissionTx self;

  public QuestionGenerationBatchUserSubmissionTx(
      QuestionGenerationBatchPlanningService planningService,
      QuestionGenerationBatchSubmissionService submissionService,
      @Lazy QuestionGenerationBatchUserSubmissionTx self) {
    this.planningService = planningService;
    this.submissionService = submissionService;
    this.self = self;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public DueUserSubmissionOutcome processDueUser(User user, Timestamp currentTime) {
    Optional<QuestionGenerationBatch> plannedBatch =
        self.planAndCommitLocalBatchForUser(user, currentTime);
    if (plannedBatch.isEmpty()) {
      return DueUserSubmissionOutcome.skipped(user.getId());
    }

    QuestionGenerationBatch batch = plannedBatch.get();
    boolean submitted = submissionService.submitPlannedBatch(batch, currentTime);
    if (submitted) {
      return DueUserSubmissionOutcome.submitted(
          user.getId(), batch.getId(), batch.getOpenaiBatchId());
    }
    return DueUserSubmissionOutcome.failed(user.getId(), batch.getId());
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Optional<QuestionGenerationBatch> planAndCommitLocalBatchForUser(
      User user, Timestamp currentTime) {
    return planningService.planLocalBatchForUser(user, currentTime);
  }

  public enum OutcomeKind {
    SKIPPED,
    SUBMITTED,
    FAILED
  }

  public record DueUserSubmissionOutcome(
      OutcomeKind kind, Integer userId, Integer localBatchId, String openAiBatchId) {

    static DueUserSubmissionOutcome skipped(Integer userId) {
      return new DueUserSubmissionOutcome(OutcomeKind.SKIPPED, userId, null, null);
    }

    static DueUserSubmissionOutcome submitted(
        Integer userId, Integer localBatchId, String openAiBatchId) {
      return new DueUserSubmissionOutcome(
          OutcomeKind.SUBMITTED, userId, localBatchId, openAiBatchId);
    }

    static DueUserSubmissionOutcome failed(Integer userId, Integer localBatchId) {
      return new DueUserSubmissionOutcome(OutcomeKind.FAILED, userId, localBatchId, null);
    }
  }
}
