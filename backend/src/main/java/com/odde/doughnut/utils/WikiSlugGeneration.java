package com.odde.doughnut.utils;

import com.github.slugify.Slugify;
import java.util.Objects;
import java.util.Set;

public final class WikiSlugGeneration {

  private static final String FALLBACK = "untitled";

  private static final Slugify SLUGIFY = Slugify.builder().build();

  private WikiSlugGeneration() {}

  public static String toBaseSlug(String input) {
    String raw = Objects.toString(input, "");
    String slug = SLUGIFY.slugify(raw.trim());
    if (slug.isEmpty()) {
      return FALLBACK;
    }
    return slug;
  }

  public static String uniqueSlugWithin(String rawTitleOrName, Set<String> siblingSlugs) {
    Objects.requireNonNull(siblingSlugs, "siblingSlugs");
    String base = toBaseSlug(rawTitleOrName);
    String candidate = base;
    int n = 2;
    while (siblingSlugs.contains(candidate)) {
      candidate = base + "-" + n;
      n++;
    }
    return candidate;
  }

  /**
   * Like {@link #uniqueSlugWithin} but each candidate is truncated so {@code candidate.length() <=
   * maxLen}. Use when the owning path prefix already consumes most of the slug column length.
   */
  public static String uniqueSlugWithinMaxLen(
      String rawSeed, Set<String> siblingSlugs, int maxLen) {
    Objects.requireNonNull(siblingSlugs, "siblingSlugs");
    if (maxLen < 1) {
      throw new IllegalArgumentException("maxLen must be >= 1");
    }
    String base = toBaseSlug(rawSeed);
    String candidate = truncateToLen(base, maxLen);
    int n = 2;
    while (siblingSlugs.contains(candidate)) {
      String suffix = "-" + n;
      int budget = maxLen - suffix.length();
      if (budget < 1) {
        throw new IllegalStateException("Cannot uniquify slug within maxLen=" + maxLen);
      }
      candidate = truncateToLen(base, budget) + suffix;
      n++;
    }
    return candidate;
  }

  private static String truncateToLen(String s, int maxLen) {
    if (s.length() <= maxLen) {
      return s;
    }
    return s.substring(0, maxLen);
  }
}
