package com.odde.doughnut.models;

import com.odde.doughnut.controllers.json.QuizQuestion;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionGenerator;
import com.odde.doughnut.services.AiAdvisorService;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record ReviewPointModel(ReviewPoint entity, ModelFactoryService modelFactoryService) {

  public ReviewPoint getEntity() {
    return entity;
  }

  public void initialReview(Timestamp currentUTCTimestamp, User user) {
    entity.setUser(user);
    entity.setInitialReviewedAt(currentUTCTimestamp);
    entity.setLastReviewedAt(currentUTCTimestamp);
    updateForgettingCurve(0);
  }

  public QuizQuestion generateAQuizQuestion(
      Randomizer randomizer, User user, AiAdvisorService aiAdvisorService) {
    QuizQuestionEntity quizQuestionEntity =
        getQuizQuestionGenerator(randomizer, aiAdvisorService)
            .generateAQuestionOfFirstPossibleType(shuffleAvailableQuestionTypes(randomizer, user))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No question generated"));
    modelFactoryService.quizQuestionRepository.save(quizQuestionEntity);
    return modelFactoryService.toQuizQuestion(quizQuestionEntity, user);
  }

  private List<QuizQuestionEntity.QuestionType> shuffleAvailableQuestionTypes(
      Randomizer randomizer, User user) {
    return randomizer.shuffle(entity.availableQuestionTypes(user.getAiQuestionTypeOnlyForReview()));
  }

  private QuizQuestionGenerator getQuizQuestionGenerator(
      Randomizer randomizer, AiAdvisorService aiAdvisorService) {
    QuizQuestionGenerator quizQuestionGenerator =
        new QuizQuestionGenerator(
            entity.getUser(), entity.getThing(), randomizer, modelFactoryService, aiAdvisorService);
    return quizQuestionGenerator;
  }

  public void markAsRepeated(Timestamp currentUTCTimestamp, boolean successful) {
    entity.setRepetitionCount(entity.getRepetitionCount() + 1);
    if (successful) {
      entity.reviewedSuccessfully(currentUTCTimestamp);
    } else {
      entity.reviewFailed(currentUTCTimestamp);
    }
    this.modelFactoryService.reviewPointRepository.save(entity);
  }

  public void updateForgettingCurve(int adjustment) {
    entity.setForgettingCurveIndex(entity.getForgettingCurveIndex() + adjustment);
    entity.setNextReviewAt(entity.calculateNextReviewAt());
    this.modelFactoryService.reviewPointRepository.save(entity);
  }
}
