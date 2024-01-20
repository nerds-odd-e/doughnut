package com.odde.doughnut.entities.validators;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Link.LinkType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidateLinkTypeValidator implements ConstraintValidator<ValidateLinkType, Object> {

  @Override
  public void initialize(ValidateLinkType constraintAnnotation) {}

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    final Link link = (Link) value;
    if (link.getLinkType() != LinkType.NO_LINK) {
      return true;
    }

    context.disableDefaultConstraintViolation();
    context
        .buildConstraintViolationWithTemplate(getMessage())
        .addPropertyNode("linkType")
        .addConstraintViolation();
    return false;
  }

  private String getMessage() {
    return "cannot use no link type for link";
  }
}
