package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.services.AiAdvisorService;
import com.odde.doughnut.services.QuestionType;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record QuizQuestionGenerator(
    User user,
    Note note,
    Randomizer randomizer,
    ModelFactoryService modelFactoryService,
    AiAdvisorService aiAdvisorService) {

  public Optional<QuizQuestionEntity> buildQuizQuestion(QuestionType questionType) {
    try {
      QuizQuestionServant servant =
          new QuizQuestionServant(user, randomizer, modelFactoryService, aiAdvisorService);
      QuizQuestionEntity quizQuestion = questionType.buildQuizQuestion(note, servant);
      return Optional.of(quizQuestion);
    } catch (QuizQuestionNotPossibleException e) {
      return Optional.empty();
    }
  }

  public QuizQuestionEntity generateAQuestionOfFirstPossibleType(List<QuestionType> shuffled) {
    QuizQuestionEntity quizQuestionEntity =
        shuffled.stream()
            .map(this::buildQuizQuestion)
            .flatMap(Optional::stream)
            .findFirst()
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No question generated"));
    modelFactoryService.save(quizQuestionEntity);
    return quizQuestionEntity;
  }

  public QuizQuestionEntity generateAQuestionOfRandomType() {
    return generateAQuestionOfFirstPossibleType(
        randomizer.shuffle(note.getAvailableQuestionTypes(user.getAiQuestionTypeOnlyForReview())));
  }
}
