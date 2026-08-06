package com.odde.doughnut.validators;

import com.odde.doughnut.algorithms.FrontmatterAliases;
import com.odde.doughnut.algorithms.FrontmatterOverlaps;
import com.odde.doughnut.algorithms.LegacyAliasOverlapMigration;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.exceptions.ApiException;

/** Validates user-authored note markdown on content save paths. */
public final class AuthoredNoteContent {

  private AuthoredNoteContent() {}

  /**
   * Migrates legacy wiki-link items from {@code aliases} into {@code overlaps}, then validates
   * authored list properties. Returns the (possibly rewritten) content to persist.
   */
  public static String prepareContentForSave(String content) {
    String migrated = LegacyAliasOverlapMigration.migrate(content);
    assertValidForSave(migrated);
    return migrated;
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
