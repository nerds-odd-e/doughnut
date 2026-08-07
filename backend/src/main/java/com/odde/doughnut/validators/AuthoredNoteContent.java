package com.odde.doughnut.validators;

import com.odde.doughnut.algorithms.FrontmatterAliases;
import com.odde.doughnut.algorithms.FrontmatterOverlaps;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.exceptions.ApiException;

/** Validates user-authored note markdown on content save paths. */
public final class AuthoredNoteContent {

  private AuthoredNoteContent() {}

  /**
   * Validates authored list properties and returns the content to persist (unchanged when valid).
   */
  public static String prepareContentForSave(String content) {
    assertValidForSave(content);
    return content == null ? "" : content;
  }

  /** Validates authored list properties ({@code aliases}, {@code overlaps}) before save. */
  public static void assertValidForSave(String content) {
    FrontmatterAliases.authoredValidationErrorForNoteContent(content)
        .ifPresent(
            message -> {
              ApiError apiError = new ApiError(message, ApiError.ErrorType.BINDING_ERROR);
              apiError.add("aliases", message);
              throw new ApiException(apiError);
            });
    FrontmatterOverlaps.authoredValidationErrorForNoteContent(content)
        .ifPresent(
            message -> {
              ApiError apiError = new ApiError(message, ApiError.ErrorType.BINDING_ERROR);
              apiError.add("overlaps", message);
              throw new ApiException(apiError);
            });
  }
}
