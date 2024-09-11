package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.factories.AiQuestionFactory;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record QuizQuestionGenerator(
    User user, Note note, Randomizer randomizer, ModelFactoryService modelFactoryService) {

  private Optional<QuestionAndAnswer> getQuizQuestionEntity(
      QuizQuestionFactory quizQuestionFactory) {
    try {
      QuestionAndAnswer questionAndAnswer = quizQuestionFactory.buildValidQuizQuestion();
      questionAndAnswer.getQuizQuestion().setQuestionAndAnswer(questionAndAnswer);
      return Optional.of(questionAndAnswer);
    } catch (QuizQuestionNotPossibleException e) {
      return Optional.empty();
    }
  }

  private QuestionAndAnswer generateAQuestionOfFirstPossibleType(
      List<QuizQuestionFactory> quizQuestionFactoryStream) {
    return quizQuestionFactoryStream.stream()
        .map(this::getQuizQuestionEntity)
        .flatMap(Optional::stream)
        .findFirst()
        .orElse(null);
  }

  public QuestionAndAnswer generateAQuestionOfRandomType(AiQuestionGenerator questionGenerator) {
    List<QuizQuestionFactory> shuffled;
    if (note instanceof HierarchicalNote && user.getAiQuestionTypeOnlyForReview()) {
      shuffled = List.of(new AiQuestionFactory(note, questionGenerator));
    } else {
      shuffled =
          randomizer.shuffle(
              note.getQuizQuestionFactories(
                  new QuizQuestionServant(user, randomizer, modelFactoryService)));
    }
    QuestionAndAnswer result = generateAQuestionOfFirstPossibleType(shuffled);
    if (result == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No question generated");
    }

    modelFactoryService.save(result);
    return result;
  }
}
