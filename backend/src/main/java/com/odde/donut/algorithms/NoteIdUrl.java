package com.odde.donut.algorithms;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Canonical compact Donut note-show URL recognition (ADR 0005). Root-relative {@code /n<ID>} and
 * absolute HTTP(S) URLs on a configured {@link CanonicalDonutOrigin} — not retired {@code /n/<ID>}
 * redirects, property paths, or query/hash variants.
 */
public final class NoteIdUrl {

  /** Exact root-relative compact note-show path. */
  private static final Pattern ROOT_RELATIVE_NOTE_HREF = Pattern.compile("^/n(\\d+)$");

  /**
   * Whole Markdown link {@code [display](href)} used when rebuilding from a stored authored link.
   */
  private static final Pattern AUTHORED_MARKDOWN_LINK =
      Pattern.compile("^\\[([^\\]]*)]\\(([^)]+)\\)$");

  /**
   * Markdown link occurrence; negative lookbehind skips the second {@code [} of wiki {@code [[…]]}.
   */
  static final Pattern MARKDOWN_LINK = Pattern.compile("(?<!\\[)\\[([^\\]]*)]\\(([^)]+)\\)");

  private NoteIdUrl() {}

  /** Note id when {@code href} is exactly the canonical compact path {@code /n<digits>}. */
  public static Optional<Integer> noteIdFromRootRelativeHref(String href) {
    if (href == null) {
      return Optional.empty();
    }
    Matcher m = ROOT_RELATIVE_NOTE_HREF.matcher(href);
    if (!m.matches()) {
      return Optional.empty();
    }
    return Optional.of(Integer.parseInt(m.group(1)));
  }

  /**
   * Note id when {@code href} is a root-relative canonical path or an exact HTTP(S) URL on {@code
   * canonicalOrigin} with that path (no query or fragment).
   */
  public static Optional<Integer> noteIdFromHref(
      String href, CanonicalDonutOrigin canonicalOrigin) {
    Optional<Integer> rootRelative = noteIdFromRootRelativeHref(href);
    if (rootRelative.isPresent()) {
      return rootRelative;
    }
    return noteIdFromAbsoluteCanonicalHref(href, canonicalOrigin);
  }

  /**
   * When {@code authoredLink} is exactly {@code [display](recognized-href)}, rebuilds the note-ID
   * URL reference; otherwise empty (e.g. wiki inners stored without Markdown brackets).
   */
  public static Optional<AuthoredNoteReference.NoteIdUrlTarget> tryParseAuthoredMarkdownLink(
      String authoredLink, CanonicalDonutOrigin canonicalOrigin) {
    if (authoredLink == null || authoredLink.isEmpty()) {
      return Optional.empty();
    }
    Matcher m = AUTHORED_MARKDOWN_LINK.matcher(authoredLink);
    if (!m.matches()) {
      return Optional.empty();
    }
    String href = m.group(2);
    return noteIdFromHref(href, canonicalOrigin)
        .map(
            noteId ->
                new AuthoredNoteReference.NoteIdUrlTarget(authoredLink, noteId, href, m.group(1)));
  }

  /** True when {@code authoredLink} is a recognized Markdown note-ID URL spelling. */
  public static boolean isAuthoredMarkdownNoteIdUrl(
      String authoredLink, CanonicalDonutOrigin canonicalOrigin) {
    return tryParseAuthoredMarkdownLink(authoredLink, canonicalOrigin).isPresent();
  }

  static Optional<AuthoredNoteReference.NoteIdUrlTarget> fromMarkdownLinkMatch(
      String fullMatch, String display, String href, CanonicalDonutOrigin canonicalOrigin) {
    return noteIdFromHref(href, canonicalOrigin)
        .map(noteId -> new AuthoredNoteReference.NoteIdUrlTarget(fullMatch, noteId, href, display));
  }

  private static Optional<Integer> noteIdFromAbsoluteCanonicalHref(
      String href, CanonicalDonutOrigin canonicalOrigin) {
    if (href == null || canonicalOrigin == null) {
      return Optional.empty();
    }
    final URI uri;
    try {
      uri = URI.create(href);
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
    String scheme = uri.getScheme();
    if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
      return Optional.empty();
    }
    if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
      return Optional.empty();
    }
    if (!canonicalOrigin.matchesHrefOrigin(uri)) {
      return Optional.empty();
    }
    return noteIdFromRootRelativeHref(uri.getRawPath());
  }
}
