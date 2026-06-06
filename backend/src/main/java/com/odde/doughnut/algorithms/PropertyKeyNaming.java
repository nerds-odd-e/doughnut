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
