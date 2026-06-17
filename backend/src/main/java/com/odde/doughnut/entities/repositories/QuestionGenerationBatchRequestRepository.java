package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.QuestionGenerationBatchRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QuestionGenerationBatchRequestRepository
    extends JpaRepository<QuestionGenerationBatchRequest, Integer> {

  List<QuestionGenerationBatchRequest> findByBatch_Id(Integer batchId);

  void deleteByBatch_Id(Integer batchId);

  @Query("SELECT r.status, COUNT(r) FROM QuestionGenerationBatchRequest r GROUP BY r.status")
  List<Object[]> countByStatus();
}
