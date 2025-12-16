package com.odde.doughnut.validators;

import com.odde.doughnut.entities.Note;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidateNoteTargetTitleValidator
    implements ConstraintValidator<ValidateNoteTargetTitle, Note> {

  @Override
  public void initialize(ValidateNoteTargetTitle constraintAnnotation) {}

  @Override
  public boolean isValid(Note note, ConstraintValidatorContext context) {
    if (note == null) {
      return true;
    }

    if (note.getTargetNote() != null && note.getTitle() != null) {
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate("Note with targetNote must have null title")
          .addPropertyNode("title")
          .addConstraintViolation();
      return false;
    }

    return true;
  }
}
