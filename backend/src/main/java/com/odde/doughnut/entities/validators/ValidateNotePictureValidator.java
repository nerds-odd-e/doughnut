package com.odde.doughnut.entities.validators;

import com.odde.doughnut.entities.NoteAccessories;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.logging.log4j.util.Strings;

public class ValidateNotePictureValidator
    implements ConstraintValidator<ValidateNotePicture, Object> {

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    final NoteAccessories noteAccessories = (NoteAccessories) value;
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
