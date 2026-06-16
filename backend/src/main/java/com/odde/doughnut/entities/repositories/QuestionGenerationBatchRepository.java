package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
