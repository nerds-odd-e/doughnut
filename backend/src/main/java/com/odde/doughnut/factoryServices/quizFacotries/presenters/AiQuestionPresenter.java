package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.controllers.json.QuizQuestion;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionPresenter;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import java.util.List;

public class AiQuestionPresenter implements QuizQuestionPresenter {
  private final MCQWithAnswer aiQuestion;

  public AiQuestionPresenter(QuizQuestionEntity quizQuestion) {
    aiQuestion = quizQuestion.getMcqWithAnswer();
  }

  @Override
  public String stem() {
    return aiQuestion.stem;
  }

  @Override
  public String mainTopic() {
    return null;
  }

  @Override
  public List<QuizQuestion.Choice> getOptions(ModelFactoryService modelFactoryService) {
    return aiQuestion.choices.stream()
        .map(
            choice -> {
              QuizQuestion.Choice option = new QuizQuestion.Choice();
              option.setDisplay(choice);
              return option;
            })
        .toList();
  }
}
