package com.odde.doughnut.validators;

import com.odde.doughnut.controllers.dto.NoteAccessoriesDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.util.Strings;

public class ValidateNotePictureValidator
    implements ConstraintValidator<ValidateNotePicture, Object> {

  @Override
  public void initialize(ValidateNotePicture constraintAnnotation) {}

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    final NoteAccessoriesDTO noteAccessories = (NoteAccessoriesDTO) value;
    List<String> fieldsWithValue = new ArrayList<>();
    if (noteAccessories.getUploadPictureProxy() != null
        && !noteAccessories.getUploadPictureProxy().isEmpty()) {
      fieldsWithValue.add("uploadPicture");
    }
    if (!Strings.isEmpty(noteAccessories.getPictureUrl())) {
      fieldsWithValue.add("pictureUrl");
    }
    if (noteAccessories.getUseParentPicture()) {
      fieldsWithValue.add("useParentPicture");
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
