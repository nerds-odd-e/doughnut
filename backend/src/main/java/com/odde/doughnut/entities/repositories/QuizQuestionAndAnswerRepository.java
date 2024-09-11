package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.QuestionAndAnswer;
import org.springframework.data.repository.CrudRepository;

public interface QuizQuestionAndAnswerRepository
    extends CrudRepository<QuestionAndAnswer, Integer> {}
