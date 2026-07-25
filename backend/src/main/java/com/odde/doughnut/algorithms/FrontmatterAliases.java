package com.odde.doughnut.algorithms;

import com.odde.doughnut.validators.DisplayNamePathSeparators;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads valid frontmatter {@code aliases} list items for recall and wiki-link behavior. */
public final class FrontmatterAliases {

  private static final String ALIASES_KEY = "aliases";

  public static final String AUTHORED_ALIASES_MESSAGE =
      "aliases must be a one-level YAML list of nonblank plain alias strings or well-formed"
          + " wiki-link overlap declarations.";

  private static final Pattern INVALID_ALIAS_CHARACTERS =
      Pattern.compile("[|#^:]|\\\\|/|＼|／|[\\r\\n]");

  private FrontmatterAliases() {}

  public static List<String> fromNoteContent(String content) {
    return NoteContentMarkdown.splitLeadingFrontmatter(content == null ? "" : content)
        .map(lf -> fromFrontmatter(lf.frontmatter()))
        .orElse(List.of());
  }

  public static List<String> fromFrontmatter(Frontmatter frontmatter) {
    if (frontmatter == null) {
      return List.of();
    }
    return frontmatter
        .getSequenceItemsIgnoreCase(ALIASES_KEY)
        .map(FrontmatterAliases::validAliasesFromRawItems)
        .orElse(List.of());
  }

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
        .getSequenceItemsIgnoreCase(ALIASES_KEY)
        .map(FrontmatterAliases::overlapWikiLinkTokensFromRawItems)
        .orElse(List.of());
  }

  public static boolean matchesFromNoteContent(String content, String answer) {
    return anyMatches(fromNoteContent(content), answer);
  }

  /**
   * Returns a validation error when {@code content} has an authored {@code aliases} property that
   * is not a one-level YAML list of nonblank plain aliases or well-formed wiki-link overlap
   * declarations. Empty when absent or valid.
   */
  public static Optional<String> authoredValidationErrorForNoteContent(String content) {
    return NoteContentMarkdown.splitLeadingFrontmatter(content == null ? "" : content)
        .flatMap(lf -> authoredValidationErrorForFrontmatter(lf.frontmatter()));
  }

  private static Optional<String> authoredValidationErrorForFrontmatter(Frontmatter frontmatter) {
    if (!frontmatter.containsKeyIgnoreCase(ALIASES_KEY)) {
      return Optional.empty();
    }
    Optional<List<?>> items = frontmatter.getSequenceItemsIgnoreCase(ALIASES_KEY);
    if (items.isEmpty()) {
      return Optional.of(AUTHORED_ALIASES_MESSAGE);
    }
    return authoredValidationErrorForRawItems(items.get());
  }

  private static Optional<String> authoredValidationErrorForRawItems(List<?> items) {
    for (Object item : items) {
      Optional<String> scalar = FrontmatterPropertyValues.scalarStringFromYamlObject(item);
      if (scalar.isEmpty()) {
        return Optional.of(AUTHORED_ALIASES_MESSAGE);
      }
      String trimmed = DisplayNamePathSeparators.trimSurroundingWhitespace(scalar.get());
      if (trimmed.isBlank() || !isAcceptableAuthoredAliasItem(trimmed)) {
        return Optional.of(AUTHORED_ALIASES_MESSAGE);
      }
    }
    return Optional.empty();
  }

  private static boolean anyMatches(List<String> aliases, String answer) {
    if (answer == null) {
      return false;
    }
    String trimmed = DisplayNamePathSeparators.trimSurroundingWhitespace(answer);
    return aliases.stream().anyMatch(alias -> alias.equalsIgnoreCase(trimmed));
  }

  private static List<String> validAliasesFromRawItems(List<?> items) {
    List<String> valid = new ArrayList<>();
    for (Object item : items) {
      FrontmatterPropertyValues.scalarStringFromYamlObject(item)
          .map(DisplayNamePathSeparators::trimSurroundingWhitespace)
          .filter(s -> !s.isBlank())
          .filter(FrontmatterAliases::isValidPlainAliasText)
          .ifPresent(valid::add);
    }
    return dedupePreserveOrder(valid);
  }

  private static List<String> overlapWikiLinkTokensFromRawItems(List<?> items) {
    List<String> tokens = new ArrayList<>();
    for (Object item : items) {
      FrontmatterPropertyValues.scalarStringFromYamlObject(item)
          .map(DisplayNamePathSeparators::trimSurroundingWhitespace)
          .filter(s -> !s.isBlank())
          .filter(FrontmatterAliases::isWikiLinkAliasItem)
          .ifPresent(tokens::add);
    }
    return dedupePreserveOrder(tokens);
  }

  private static boolean isAcceptableAuthoredAliasItem(String trimmed) {
    return isWikiLinkAliasItem(trimmed) || isValidPlainAliasText(trimmed);
  }

  private static boolean isWikiLinkAliasItem(String trimmed) {
    Matcher matcher = WikiLinkMarkdown.INNER_LINK_PATTERN.matcher(trimmed);
    if (!matcher.matches()) {
      return false;
    }
    String inner = matcher.group(1).trim();
    if (inner.isEmpty()) {
      return false;
    }
    return !WikiLinkMarkdown.splitInner(inner).target().trim().isEmpty();
  }

  private static boolean isValidPlainAliasText(String trimmed) {
    if (trimmed.contains("[[") || trimmed.contains("]]")) {
      return false;
    }
    return !INVALID_ALIAS_CHARACTERS.matcher(trimmed).find();
  }

  public static String normalizedLookupKey(String alias) {
    return Normalizer.normalize(alias, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
  }

  private static List<String> dedupePreserveOrder(List<String> items) {
    List<String> out = new ArrayList<>();
    Set<String> seenNormalized = new HashSet<>();
    for (String item : items) {
      if (seenNormalized.add(normalizedLookupKey(item))) {
        out.add(item);
      }
    }
    return List.copyOf(out);
  }
}
