package com.odde.doughnut.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ReservedNoteTitleValidator
    implements ConstraintValidator<NotReservedNoteTitle, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    return !value.trim().equalsIgnoreCase("index");
  }
}
