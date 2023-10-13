package com.odde.doughnut.exceptions;

public class FeedbackExistingException extends RuntimeException {
  public FeedbackExistingException() {
    super("Feedback already exists!");
  }
}
