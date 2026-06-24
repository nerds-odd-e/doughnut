package com.odde.doughnut.validators;

import com.odde.doughnut.algorithms.FrontmatterAliases;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.exceptions.ApiException;

/** Validates user-authored note markdown on content save paths. */
public final class AuthoredNoteContent {

  private AuthoredNoteContent() {}

  public static void assertAliasesValidForSave(String content) {
    FrontmatterAliases.authoredValidationErrorForNoteContent(content)
        .ifPresent(
            message -> {
              ApiError apiError = new ApiError(message, ApiError.ErrorType.BINDING_ERROR);
              apiError.add("aliases", message);
              throw new ApiException(apiError);
            });
  }
}
