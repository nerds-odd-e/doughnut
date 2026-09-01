package com.odde.donut.algorithms;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;

/**
 * Configured canonical HTTP(S) origin for recognizing absolute Donut note URLs (ADR 0005).
 * Production default is {@link #PRODUCTION_DEFAULT}; deployments and tests override via {@code
 * donut.canonical-origin}.
 */
public final class CanonicalDonutOrigin {

  public static final String PRODUCTION_DEFAULT = "https://doughnut.odd-e.com";

  private final String value;

  private CanonicalDonutOrigin(String value) {
    this.value = value;
  }

  /**
   * Parses and normalizes an origin ({@code scheme://host[:port]}, no path, trailing slash
   * stripped).
   */
  public static CanonicalDonutOrigin parse(String origin) {
    Objects.requireNonNull(origin, "origin");
    String trimmed = stripTrailingSlash(origin.trim());
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("canonical Donut origin must not be blank");
    }
    URI uri = URI.create(trimmed);
    String scheme = uri.getScheme();
    if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
      throw new IllegalArgumentException("canonical Donut origin must be http(s): " + origin);
    }
    if (uri.getHost() == null) {
      throw new IllegalArgumentException("canonical Donut origin must include a host: " + origin);
    }
    if (uri.getRawPath() != null && !uri.getRawPath().isEmpty() && !"/".equals(uri.getRawPath())) {
      throw new IllegalArgumentException(
          "canonical Donut origin must not include a path: " + origin);
    }
    if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
      throw new IllegalArgumentException(
          "canonical Donut origin must not include query or fragment: " + origin);
    }
    return new CanonicalDonutOrigin(normalizeOrigin(uri));
  }

  public static CanonicalDonutOrigin production() {
    return parse(PRODUCTION_DEFAULT);
  }

  /** Normalized {@code scheme://host[:port]} (lowercase scheme and host). */
  public String value() {
    return value;
  }

  boolean matchesHrefOrigin(URI href) {
    if (href.getScheme() == null || href.getHost() == null) {
      return false;
    }
    return value.equals(normalizeOrigin(href));
  }

  private static String normalizeOrigin(URI uri) {
    String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
    String host = uri.getHost().toLowerCase(Locale.ROOT);
    int port = uri.getPort();
    if (port == -1) {
      return scheme + "://" + host;
    }
    return scheme + "://" + host + ":" + port;
  }

  private static String stripTrailingSlash(String s) {
    if (s.endsWith("/") && s.length() > 1) {
      return s.substring(0, s.length() - 1);
    }
    return s;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof CanonicalDonutOrigin other && value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public String toString() {
    return value;
  }
}
