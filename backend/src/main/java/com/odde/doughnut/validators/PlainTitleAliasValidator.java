package com.odde.doughnut.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PlainTitleAliasValidator implements ConstraintValidator<NoPlainTitleAlias, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    return !NoteTitleAuthoring.hasPlainTitleAliasSegments(value);
  }
}
