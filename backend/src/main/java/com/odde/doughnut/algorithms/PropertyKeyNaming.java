package com.odde.doughnut.algorithms;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Property key base names and numeric suffix conventions (`url 2`, `a part of 3`, …). */
public final class PropertyKeyNaming {

  private static final Pattern KEY_SUFFIX_PATTERN = Pattern.compile("^(.+?) (\\d+)$");

  private PropertyKeyNaming() {}

  public record BaseAndSuffix(String base, Integer suffix) {}

  /** Splits a property key into its base name and optional numeric suffix (`url 2` → suffix 2). */
  public static BaseAndSuffix propertyKeyBaseAndSuffix(String key) {
    String trimmed = key == null ? "" : key.trim();
    Matcher matcher = KEY_SUFFIX_PATTERN.matcher(trimmed);
    if (matcher.matches()) {
      int n = Integer.parseInt(matcher.group(2));
      if (n >= 2) {
        return new BaseAndSuffix(matcher.group(1), n);
      }
    }
    return new BaseAndSuffix(trimmed, null);
  }

  private static boolean propertyKeyBaseMatches(String key, String baseKey) {
    BaseAndSuffix parts = propertyKeyBaseAndSuffix(key);
    return parts.base().trim().equalsIgnoreCase(baseKey.trim());
  }

  private static boolean propertyKeyBaseMatchesAny(String key, String... baseKeys) {
    for (String baseKey : baseKeys) {
      if (propertyKeyBaseMatches(key, baseKey)) {
        return true;
      }
    }
    return false;
  }

  private static String normalizedBaseWithoutUnderscores(String key) {
    BaseAndSuffix parts = propertyKeyBaseAndSuffix(key);
    return parts.base().trim().toLowerCase().replace("_", "");
  }

  /** Rich-mode property key for header image upload ({@code image:} only). */
  public static boolean isImagePropertyKey(String key) {
    return propertyKeyBaseMatches(key, "image");
  }

  /** Rich-mode property key for image mask ({@code image_mask:}). */
  public static boolean isImageMaskPropertyKey(String key) {
    return "imagemask".equals(normalizedBaseWithoutUnderscores(key));
  }

  /** Rich-mode property key for Wikidata Q-id ({@code wikidata_id} or {@code wikidataId}). */
  public static boolean isWikidataIdPropertyKey(String key) {
    return propertyKeyBaseMatchesAny(key, "wikidata_id", "wikidataId");
  }

  public static boolean isUrlPropertyKey(String key) {
    return propertyKeyBaseMatches(key, "url");
  }

  public static boolean isExampleOfPropertyKey(String key) {
    return propertyKeyBaseMatches(key, "example of");
  }

  public static boolean isTitlePatternPropertyKey(String key) {
    return "titlepattern".equals(normalizedBaseWithoutUnderscores(key));
  }

  public static boolean isQuestionGenerationInstructionPropertyKey(String key) {
    return "questiongenerationinstruction".equals(normalizedBaseWithoutUnderscores(key));
  }

  /**
   * Structural frontmatter keys excluded from {@code note_property_index} and automatic property
   * tracker seeding.
   */
  public static boolean isReservedStructuralKey(String key) {
    return isImagePropertyKey(key)
        || isImageMaskPropertyKey(key)
        || isWikidataIdPropertyKey(key)
        || isUrlPropertyKey(key)
        || isTitlePatternPropertyKey(key)
        || isQuestionGenerationInstructionPropertyKey(key);
  }

  /**
   * Next free key in a base-key family: returns {@code baseKey} when slot 1 is free, otherwise
   * {@code baseKey N} for the smallest free {@code N >= 2}.
   */
  public static String nextAvailablePropertyKeyForBase(
      String baseKey, Iterable<String> existingKeys) {
    Set<Integer> occupied = new HashSet<>();
    for (String existingKey : existingKeys) {
      if (existingKey == null || existingKey.isBlank()) {
        continue;
      }
      if (!propertyKeyBaseMatches(existingKey, baseKey)) {
        continue;
      }
      BaseAndSuffix parts = propertyKeyBaseAndSuffix(existingKey);
      occupied.add(parts.suffix() == null ? 1 : parts.suffix());
    }
    if (!occupied.contains(1)) {
      return baseKey;
    }
    int n = 2;
    while (occupied.contains(n)) {
      n++;
    }
    return baseKey + " " + n;
  }
}
