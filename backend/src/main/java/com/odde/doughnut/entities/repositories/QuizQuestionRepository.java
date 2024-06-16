package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.QuizQuestionAndAnswer;
import org.springframework.data.repository.CrudRepository;

public interface QuizQuestionRepository extends CrudRepository<QuizQuestionAndAnswer, Integer> {}
