package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.RecallPrompt;
import org.springframework.data.repository.CrudRepository;

public interface ReviewQuestionInstanceRepository extends CrudRepository<RecallPrompt, Integer> {}
