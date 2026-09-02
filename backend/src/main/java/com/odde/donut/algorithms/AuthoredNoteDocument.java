package com.odde.donut.algorithms;

import java.util.List;

/**
 * A note's Markdown content paired with its distinct authored references in first-occurrence order.
 * Consumed by {@code Note.replaceContent} to update both in the same aggregate operation.
 * Validation and normalization belong to the content's originating write path.
 */
public record AuthoredNoteDocument(String content, List<AuthoredNoteReference> references) {

  /** Parses authored references without validating or normalizing {@code content}. */
  public static AuthoredNoteDocument fromContent(
      String content, CanonicalDonutOrigin canonicalOrigin) {
    List<AuthoredNoteReference> references =
        AuthoredNoteReferences.uniquePreserveOrder(
            AuthoredNoteReferences.inOccurrenceOrder(content, canonicalOrigin));
    return new AuthoredNoteDocument(content, references);
  }
}
