package com.odde.donut.entities.repositories;

import com.odde.donut.entities.QuestionGenerationBatchRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface QuestionGenerationBatchRequestRepository
    extends JpaRepository<QuestionGenerationBatchRequest, Integer> {

  List<QuestionGenerationBatchRequest> findByBatch_Id(Integer batchId);

  void deleteByBatch_Id(Integer batchId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Transactional
  @Query(
      """
      UPDATE QuestionGenerationBatchRequest r
      SET r.status = com.odde.donut.entities.QuestionGenerationBatchRequestStatus.FAILED,
          r.errorDetail = :errorDetail
      WHERE r.batch.id = :batchId
        AND r.status = com.odde.donut.entities.QuestionGenerationBatchRequestStatus.PENDING
      """)
  int markPendingAsFailedForBatch(
      @Param("batchId") Integer batchId, @Param("errorDetail") String errorDetail);

  @Query("SELECT r.status, COUNT(r) FROM QuestionGenerationBatchRequest r GROUP BY r.status")
  List<Object[]> countByStatus();
}
