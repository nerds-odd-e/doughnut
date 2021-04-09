package com.odde.doughnut.entities.validators;

import com.odde.doughnut.entities.NoteContent;
import org.apache.logging.log4j.util.Strings;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.List;

public class ValidateNotePictureValidator implements ConstraintValidator<ValidateNotePicture, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        final NoteContent noteContent = (NoteContent) value;
        List<String> fieldsWithValue = new ArrayList<>();
        if (noteContent.getUploadPictureProxy() != null && !noteContent.getUploadPictureProxy().isEmpty()) {
            fieldsWithValue.add("uploadPicture");
        }
        if (!Strings.isEmpty(noteContent.getPictureUrl())) {
            fieldsWithValue.add("pictureUrl");
        }
        if (noteContent.getUseParentPicture()) {
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

