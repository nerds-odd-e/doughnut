package com.odde.doughnut.validators;

import com.odde.doughnut.controllers.dto.NoteAccessoriesDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.util.Strings;

public class ValidateNoteImageValidator implements ConstraintValidator<ValidateNoteImage, Object> {

  @Override
  public void initialize(ValidateNoteImage constraintAnnotation) {}

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    final NoteAccessoriesDTO noteAccessory = (NoteAccessoriesDTO) value;
    List<String> fieldsWithValue = new ArrayList<>();
    if (noteAccessory.getUploadImage() != null && !noteAccessory.getUploadImage().isEmpty()) {
      fieldsWithValue.add("uploadImage");
    }
    if (!Strings.isEmpty(noteAccessory.getImageUrl())) {
      fieldsWithValue.add("imageUrl");
    }
    if (noteAccessory.getUseParentImage()) {
      fieldsWithValue.add("useParentImage");
    }

    if (fieldsWithValue.size() > 1) {
      context.disableDefaultConstraintViolation();
      fieldsWithValue.forEach(
          f -> {
            context
                .buildConstraintViolationWithTemplate(getMessage(fieldsWithValue))
                .addPropertyNode(f)
                .addConstraintViolation();
          });
      return false;
    }

    return true;
  }

  private String getMessage(List<String> fieldsWithValue) {
    return "cannot use together: " + String.join(", ", fieldsWithValue);
  }
}
