package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.QuestionGenerationBatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionGenerationBatchRepository
    extends JpaRepository<QuestionGenerationBatch, Integer> {}
