package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.QuestionGenerationBatchRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionGenerationBatchRequestRepository
    extends JpaRepository<QuestionGenerationBatchRequest, Integer> {

  List<QuestionGenerationBatchRequest> findByBatch_Id(Integer batchId);
}
