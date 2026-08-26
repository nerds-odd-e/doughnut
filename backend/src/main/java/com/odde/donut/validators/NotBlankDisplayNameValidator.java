package com.odde.donut.validators;

import com.odde.donut.entities.DisplayName;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotBlankDisplayNameValidator
    implements ConstraintValidator<NotBlankDisplayName, String> {

  private boolean allowNull;

  @Override
  public void initialize(NotBlankDisplayName constraintAnnotation) {
    allowNull = constraintAnnotation.allowNull();
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return allowNull;
    }
    return !new DisplayName(value).value().isEmpty();
  }
}
