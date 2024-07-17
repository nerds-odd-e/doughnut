package com.odde.doughnut.exceptions;

import com.odde.doughnut.controllers.dto.ApiError;

public class AssessmentAttemptLimitException extends ApiException {
  public AssessmentAttemptLimitException(String message) {
    super(message, ApiError.ErrorType.ATTEMPT_LIMIT_ERROR, "error");
  }
}
