package com.odde.donut.exceptions;

import com.odde.donut.controllers.dto.ApiError;

public class QuestionAnswerException extends ApiException {
  public QuestionAnswerException(String message) {
    super(message, ApiError.ErrorType.QUESTION_ANSWER_ERROR, "Q&A exception: " + message);
  }
}
