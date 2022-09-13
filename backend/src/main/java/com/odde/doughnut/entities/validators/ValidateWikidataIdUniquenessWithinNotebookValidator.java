package com.odde.doughnut.entities.validators;

import com.odde.doughnut.entities.Note;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ValidateWikidataIdUniquenessWithinNotebookValidator
    implements ConstraintValidator<ValidateWikidataIdUniquenessWithinNotebook, Note> {
  @Override
  public boolean isValid(Note value, ConstraintValidatorContext context) {
    return true;
  }
}
