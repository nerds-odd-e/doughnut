package com.odde.doughnut.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ReservedNoteTitleValidator.class)
public @interface NotReservedNoteTitle {
  String message() default "'index' is reserved for notebook and folder index content.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
