package com.odde.donut.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotBlankDisplayNameValidator.class)
public @interface NotBlankDisplayName {
  String message() default "{jakarta.validation.constraints.NotBlank.message}";

  boolean allowNull() default false;

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
