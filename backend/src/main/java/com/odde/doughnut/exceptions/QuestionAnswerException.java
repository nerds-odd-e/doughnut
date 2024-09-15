package com.odde.doughnut.exceptions;

import com.odde.doughnut.controllers.dto.ApiError;

public class QuestionAnswerException extends ApiException {
  public QuestionAnswerException(String message) {
    super(message, ApiError.ErrorType.QUESTION_ANSWER_ERROR, "Q&A exception: " + message);
  }
}
