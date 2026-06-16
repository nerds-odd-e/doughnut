package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionGenerationBatchRepository
    extends JpaRepository<QuestionGenerationBatch, Integer> {

  List<QuestionGenerationBatch> findByStatus(QuestionGenerationBatchStatus status);

  List<QuestionGenerationBatch> findByStatusAndOutputCollectedAtIsNull(
      QuestionGenerationBatchStatus status);

  List<QuestionGenerationBatch> findByStatusAndOutputCollectedAtIsNotNullAndImportedAtIsNull(
      QuestionGenerationBatchStatus status);

  boolean existsByUser_IdAndStatus(Integer userId, QuestionGenerationBatchStatus status);

  boolean existsByUser_IdAndOpenaiBatchIdIsNotNullAndStatusIn(
      Integer userId, Collection<QuestionGenerationBatchStatus> statuses);

  @Query(
      """
      SELECT b FROM QuestionGenerationBatch b
      WHERE (b.status IN :failureStatuses
             AND COALESCE(b.submittedAt, b.plannedAt) < :cutoff)
         OR (b.status = :completedStatus
             AND b.importedAt IS NOT NULL
             AND b.importedAt < :cutoff)
      """)
  List<QuestionGenerationBatch> findPrunableTerminalBatchesOlderThan(
      @Param("failureStatuses") Collection<QuestionGenerationBatchStatus> failureStatuses,
      @Param("completedStatus") QuestionGenerationBatchStatus completedStatus,
      @Param("cutoff") Timestamp cutoff);
}
