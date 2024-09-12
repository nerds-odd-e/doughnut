package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.ReviewQuestionInstance;
import org.springframework.data.repository.CrudRepository;

public interface QuizQuestionRepository extends CrudRepository<ReviewQuestionInstance, Integer> {}
