package com.odde.donut.exceptions;

public class UnexpectedNoAccessRightException extends Exception {
  public UnexpectedNoAccessRightException() {
    super("It seems you cannot access this page.");
  }
}
