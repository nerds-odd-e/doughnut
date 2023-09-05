package com.odde.doughnut.services;

import com.odde.doughnut.entities.MarkedQuestion;
import com.odde.doughnut.entities.json.MarkedQuestionRequest;

public class MarkedQuestionService {

  public MarkedQuestionService() {
  }

  public MarkedQuestion markQuestion(MarkedQuestionRequest markedQuestionRequest) {
    MarkedQuestion markedQuestion = new MarkedQuestion();
    markedQuestion.setQuizQuestionId(markedQuestionRequest.quizQuestionId);
    markedQuestion.setNoteId(markedQuestionRequest.noteId);
    markedQuestion.setIsGood(markedQuestionRequest.isGood);
    return markedQuestion;
  }
}
