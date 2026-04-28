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
}
