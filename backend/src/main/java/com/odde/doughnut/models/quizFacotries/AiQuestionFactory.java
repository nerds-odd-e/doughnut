package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.models.NoteModel;
import com.odde.doughnut.services.AiAdvisorService;

public class AiQuestionFactory implements QuizQuestionFactory {
  private ReviewPoint reviewPoint;
  private QuizQuestionServant servant;

  public AiQuestionFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    this.reviewPoint = reviewPoint;
    this.servant = servant;
  }

  @Override
  public void fillQuizQuestion(QuizQuestion quizQuestion, AiAdvisorService aiAdvisorService) {
    NoteModel noteModel = servant.modelFactoryService.toNoteModel(reviewPoint.getNote());
    String suggestion =
        aiAdvisorService.generateQuestion(noteModel.getChatMessagesForGenerateQuestion());
    quizQuestion.setRawJsonQuestion(suggestion);
  }
}
