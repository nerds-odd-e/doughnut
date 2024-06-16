package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.QuizQuestionAndAnswer;
import org.springframework.data.repository.CrudRepository;

public interface QuizQuestionAndAnswerRepository
    extends CrudRepository<QuizQuestionAndAnswer, Integer> {}
