package com.odde.doughnut.validators;

public final class DisplayNamePathSeparators {

  private DisplayNamePathSeparators() {}

  public static final String REGEXP = "^[^\\\\/:]*$";

  public static final String MESSAGE = "Name must not contain backslash, slash, or colon.";
}
