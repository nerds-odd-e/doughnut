package com.odde.donut.algorithms;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Canonical compact Donut note-show URL recognition (ADR 0005). Root-relative {@code /n<ID>} only —
 * not retired {@code /n/<ID>} redirects, property paths, or query/hash variants.
 */
public final class NoteIdUrl {

  /** Exact root-relative compact note-show path. */
  private static final Pattern ROOT_RELATIVE_NOTE_HREF = Pattern.compile("^/n(\\d+)$");

  /**
   * Whole Markdown link whose href is a recognized root-relative note URL: {@code [display](/nID)}.
   */
  private static final Pattern AUTHORED_MARKDOWN_NOTE_ID_LINK =
      Pattern.compile("^\\[([^\\]]*)]\\((/n\\d+)\\)$");

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
   * When {@code authoredLink} is exactly {@code [display](/nID)}, rebuilds the note-ID URL
   * reference; otherwise empty (e.g. wiki inners stored without Markdown brackets).
   */
  public static Optional<AuthoredNoteReference.NoteIdUrlTarget> tryParseAuthoredMarkdownLink(
      String authoredLink) {
    if (authoredLink == null || authoredLink.isEmpty()) {
      return Optional.empty();
    }
    Matcher m = AUTHORED_MARKDOWN_NOTE_ID_LINK.matcher(authoredLink);
    if (!m.matches()) {
      return Optional.empty();
    }
    String href = m.group(2);
    return noteIdFromRootRelativeHref(href)
        .map(
            noteId ->
                new AuthoredNoteReference.NoteIdUrlTarget(authoredLink, noteId, href, m.group(1)));
  }

  /** True when {@code authoredLink} is a recognized Markdown note-ID URL spelling. */
  public static boolean isAuthoredMarkdownNoteIdUrl(String authoredLink) {
    return tryParseAuthoredMarkdownLink(authoredLink).isPresent();
  }

  static Optional<AuthoredNoteReference.NoteIdUrlTarget> fromMarkdownLinkMatch(
      String fullMatch, String display, String href) {
    return noteIdFromRootRelativeHref(href)
        .map(noteId -> new AuthoredNoteReference.NoteIdUrlTarget(fullMatch, noteId, href, display));
  }
}
