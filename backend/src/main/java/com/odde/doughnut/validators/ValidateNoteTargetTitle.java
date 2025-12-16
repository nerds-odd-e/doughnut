package com.odde.doughnut.validators;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({TYPE, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = ValidateNoteTargetTitleValidator.class)
public @interface ValidateNoteTargetTitle {
  String message() default "Note with targetNote must have null title";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
