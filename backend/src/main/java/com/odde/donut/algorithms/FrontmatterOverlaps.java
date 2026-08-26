package com.odde.donut.algorithms;

import com.odde.donut.validators.DisplayNamePathSeparators;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Authored frontmatter {@code overlaps} list: validation and overlap grading tokens. */
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

  /**
   * Wiki-link tokens from the authored {@code overlaps} list only (occurrence order, normalized
   * dedupe).
   */
  public static List<String> overlapWikiLinkTokensFromNoteContent(String content) {
    return NoteContentMarkdown.splitLeadingFrontmatter(content == null ? "" : content)
        .map(lf -> overlapWikiLinkTokensFromFrontmatter(lf.frontmatter()))
        .orElse(List.of());
  }

  public static List<String> overlapWikiLinkTokensFromFrontmatter(Frontmatter frontmatter) {
    if (frontmatter == null) {
      return List.of();
    }
    return frontmatter
        .getSequenceItemsIgnoreCase(OVERLAPS_KEY)
        .map(FrontmatterOverlaps::overlapWikiLinkTokensFromRawItems)
        .orElse(List.of());
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

  private static List<String> overlapWikiLinkTokensFromRawItems(List<?> items) {
    List<String> tokens = new ArrayList<>();
    for (Object item : items) {
      FrontmatterPropertyValues.scalarStringFromYamlObject(item)
          .map(DisplayNamePathSeparators::trimSurroundingWhitespace)
          .filter(s -> !s.isBlank())
          .filter(WikiLinkMarkdown::isWellFormedWholeLinkToken)
          .ifPresent(tokens::add);
    }
    return dedupePreserveOrder(tokens);
  }

  private static List<String> dedupePreserveOrder(List<String> items) {
    List<String> out = new ArrayList<>();
    Set<String> seenNormalized = new HashSet<>();
    for (String item : items) {
      if (seenNormalized.add(FrontmatterAliases.normalizedLookupKey(item))) {
        out.add(item);
      }
    }
    return List.copyOf(out);
  }
}
