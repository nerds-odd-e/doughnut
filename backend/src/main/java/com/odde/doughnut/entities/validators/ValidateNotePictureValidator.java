package com.odde.doughnut.entities.validators;

import com.odde.doughnut.entities.NoteEntity;
import org.apache.logging.log4j.util.Strings;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.List;

public class ValidateNotePictureValidator implements ConstraintValidator<ValidateNotePicture, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        final NoteEntity noteEntity = (NoteEntity) value;
        List<String> fieldsWithValue = new ArrayList<>();
        if (noteEntity.getUploadPictureProxy() != null && !noteEntity.getUploadPictureProxy().isEmpty()) {
            fieldsWithValue.add("uploadPicture");
        }
        if (!Strings.isEmpty(noteEntity.getPicture())) {
            fieldsWithValue.add("picture");
        }
        if (noteEntity.getUseParentPicture()) {
            fieldsWithValue.add("useParentPicture");
        }

        if (fieldsWithValue.size() > 1) {
            context.disableDefaultConstraintViolation();
            fieldsWithValue.forEach(f-> {
                context.buildConstraintViolationWithTemplate(getMessage(fieldsWithValue)).addPropertyNode(f).addConstraintViolation();
            });
            return false;
        }

        return true;
    }

    private String getMessage(List<String> fieldsWithValue) {
        return "cannot use together: " + String.join(", ", fieldsWithValue);
    }
}

