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
    if (!ReservedReadmeTitles.isReserved(value)) {
      return true;
    }
    context.disableDefaultConstraintViolation();
    context
        .buildConstraintViolationWithTemplate(ReservedReadmeTitles.RESERVED_MESSAGE)
        .addConstraintViolation();
    return false;
  }
}
