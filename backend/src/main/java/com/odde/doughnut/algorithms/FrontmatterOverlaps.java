package com.odde.doughnut.algorithms;

import com.odde.doughnut.validators.DisplayNamePathSeparators;
import java.util.List;
import java.util.Optional;

/** Validates authored frontmatter {@code overlaps} list items (wiki-link only). */
public final class FrontmatterOverlaps {

  private static final String OVERLAPS_KEY = "overlaps";

  public static final String AUTHORED_OVERLAPS_MESSAGE =
      "overlaps must be a one-level YAML list of well-formed wiki-link items.";

  private FrontmatterOverlaps() {}

  /**
   * Returns a validation error when {@code content} has an authored {@code overlaps} property that
   * is not a one-level YAML list of well-formed wiki-link items. Empty when absent or valid.
   */
  public static Optional<String> authoredValidationErrorForNoteContent(String content) {
    return NoteContentMarkdown.splitLeadingFrontmatter(content == null ? "" : content)
        .flatMap(lf -> authoredValidationErrorForFrontmatter(lf.frontmatter()));
  }

  private static Optional<String> authoredValidationErrorForFrontmatter(Frontmatter frontmatter) {
    if (!frontmatter.containsKeyIgnoreCase(OVERLAPS_KEY)) {
      return Optional.empty();
    }
    Optional<List<?>> items = frontmatter.getSequenceItemsIgnoreCase(OVERLAPS_KEY);
    if (items.isEmpty()) {
      return Optional.of(AUTHORED_OVERLAPS_MESSAGE);
    }
    return authoredValidationErrorForRawItems(items.get());
  }

  private static Optional<String> authoredValidationErrorForRawItems(List<?> items) {
    for (Object item : items) {
      Optional<String> scalar = FrontmatterPropertyValues.scalarStringFromYamlObject(item);
      if (scalar.isEmpty()) {
        return Optional.of(AUTHORED_OVERLAPS_MESSAGE);
      }
      String trimmed = DisplayNamePathSeparators.trimSurroundingWhitespace(scalar.get());
      if (trimmed.isBlank() || !WikiLinkMarkdown.isWellFormedWholeLinkToken(trimmed)) {
        return Optional.of(AUTHORED_OVERLAPS_MESSAGE);
      }
    }
    return Optional.empty();
  }
}
