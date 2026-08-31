package com.odde.donut.validators;

import com.odde.donut.algorithms.FrontmatterAliases;
import com.odde.donut.algorithms.FrontmatterNoteLevel;
import com.odde.donut.algorithms.FrontmatterOverlaps;
import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.exceptions.ApiException;
import java.util.Optional;

/** Validates user-authored note markdown on content save paths. */
public final class AuthoredNoteContent {

  private AuthoredNoteContent() {}

  /** Validates authored properties and returns the content to persist (unchanged when valid). */
  public static String prepareContentForSave(String content) {
    assertValidForSave(content);
    return content == null ? "" : content;
  }

  /** Validates authored properties ({@code aliases}, {@code overlaps}, {@code note_level}). */
  public static void assertValidForSave(String content) {
    throwBindingError("aliases", FrontmatterAliases.authoredValidationErrorForNoteContent(content));
    throwBindingError(
        "overlaps", FrontmatterOverlaps.authoredValidationErrorForNoteContent(content));
    throwBindingError(
        "note_level", FrontmatterNoteLevel.authoredValidationErrorForNoteContent(content));
  }

  private static void throwBindingError(String field, Optional<String> error) {
    error.ifPresent(
        message -> {
          ApiError apiError = new ApiError(message, ApiError.ErrorType.BINDING_ERROR);
          apiError.add(field, message);
          throw new ApiException(apiError);
        });
  }
}
