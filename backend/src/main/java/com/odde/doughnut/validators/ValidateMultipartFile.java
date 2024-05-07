package com.odde.doughnut.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MultipartFileValidator.class)
public @interface ValidateMultipartFile {
  String message() default "Invalid audio file";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  String[] allowedTypes();

  long maxSize() default 10 * 1024 * 1024; // 10 MB
}
