package com.odde.donut.algorithms;

import java.util.List;

/**
 * A note's Markdown content paired with its distinct authored references in first-occurrence order.
 * Consumed by {@code Note.replaceContent} to update both in the same aggregate operation.
 * Validation belongs to the content's originating write path.
 */
public record AuthoredNoteDocument(String content, List<AuthoredNoteReference> references) {

  /**
   * Normalizes CRLF line endings to LF, so stored and committed note Markdown is LF whatever its
   * source, then parses authored references. Does not validate {@code content}.
   */
  public static AuthoredNoteDocument fromContent(
      String content, CanonicalDonutOrigin canonicalOrigin) {
    String lfContent = content == null ? null : content.replace("\r\n", "\n");
    List<AuthoredNoteReference> references =
        AuthoredNoteReferences.uniquePreserveOrder(
            AuthoredNoteReferences.inOccurrenceOrder(lfContent, canonicalOrigin));
    return new AuthoredNoteDocument(lfContent, references);
  }
}
