package com.odde.doughnut.algorithms;

import com.odde.doughnut.validators.DisplayNamePathSeparators;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Reads valid frontmatter {@code aliases} list items for recall and wiki-link behavior. */
public final class FrontmatterAliases {

  private static final String ALIASES_KEY = "aliases";

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

  public static boolean matchesFromNoteContent(String content, String answer) {
    return anyMatches(fromNoteContent(content), answer);
  }

  private static boolean anyMatches(List<String> aliases, String answer) {
    if (answer == null) {
      return false;
    }
    String stripped = answer.strip();
    return aliases.stream().anyMatch(alias -> alias.equalsIgnoreCase(stripped));
  }

  private static List<String> validAliasesFromRawItems(List<?> items) {
    List<String> valid = new ArrayList<>();
    for (Object item : items) {
      FrontmatterPropertyValues.scalarStringFromYamlObject(item)
          .map(DisplayNamePathSeparators::trimSurroundingWhitespace)
          .filter(s -> !s.isBlank())
          .filter(FrontmatterAliases::isValidAliasText)
          .ifPresent(valid::add);
    }
    return dedupePreserveOrder(valid);
  }

  private static boolean isValidAliasText(String trimmed) {
    if (trimmed.contains("[[") || trimmed.contains("]]")) {
      return false;
    }
    return !INVALID_ALIAS_CHARACTERS.matcher(trimmed).find();
  }

  private static List<String> dedupePreserveOrder(List<String> items) {
    List<String> out = new ArrayList<>();
    Set<String> seenNormalized = new HashSet<>();
    for (String item : items) {
      String key = Normalizer.normalize(item, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
      if (seenNormalized.add(key)) {
        out.add(item);
      }
    }
    return List.copyOf(out);
  }
}
