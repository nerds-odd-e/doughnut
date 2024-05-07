package com.odde.doughnut.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

public class MultipartFileValidator
    implements ConstraintValidator<ValidateMultipartFile, MultipartFile> {

  private String[] allowedContentTypes;
  private long maxFileSize;

  @Override
  public void initialize(ValidateMultipartFile constraintAnnotation) {
    this.allowedContentTypes = constraintAnnotation.allowedTypes();
    this.maxFileSize = constraintAnnotation.maxSize();
  }

  @Override
  public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
    if (file == null) {
      return true;
    }

    String contentType = file.getContentType();
    long size = file.getSize();

    if (contentType == null || size <= 0) {
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate("Invalid file: content type or size is not valid.")
          .addConstraintViolation();
      return false;
    }

    boolean isAllowedContentType = false;
    for (String allowedContentType : allowedContentTypes) {
      if (allowedContentType.equals(contentType)) {
        isAllowedContentType = true;
        break;
      }
    }

    if (!isAllowedContentType) {
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate(
              "Invalid file type: "
                  + contentType
                  + ". Allowed types are: "
                  + String.join(", ", allowedContentTypes))
          .addConstraintViolation();
      return false;
    }

    if (size > maxFileSize) {
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate(
              "File size exceeds the limit: " + maxFileSize + " bytes.")
          .addConstraintViolation();
      return false;
    }

    return true;
  }
}
