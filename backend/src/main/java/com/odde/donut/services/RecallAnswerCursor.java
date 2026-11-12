package com.odde.donut.services;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;

/**
 * Where a window of recall answers continues: the window fixed by its first call and the position
 * of the last answer already returned, so answers recorded while paging neither repeat nor shift.
 */
public record RecallAnswerCursor(
    Instant from, Instant to, Instant lastAnsweredAt, int lastAnswerId) {
  private static final String SEPARATOR = "|";

  /** Positioned after every answer up to {@code to}, so the page starts at the newest. */
  static RecallAnswerCursor firstPage(Instant from, Instant to) {
    return new RecallAnswerCursor(from, to, to, Integer.MAX_VALUE);
  }

  public String encode() {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(
            String.join(
                    SEPARATOR,
                    from.toString(),
                    to.toString(),
                    lastAnsweredAt.toString(),
                    String.valueOf(lastAnswerId))
                .getBytes(StandardCharsets.UTF_8));
  }

  /**
   * @throws IllegalArgumentException when the text is not a cursor this service gave
   */
  public static RecallAnswerCursor decode(String text) {
    String[] parts =
        new String(Base64.getUrlDecoder().decode(text), StandardCharsets.UTF_8)
            .split("\\" + SEPARATOR);
    if (parts.length != 4) {
      throw new IllegalArgumentException("Not a recall answer cursor: " + text);
    }
    try {
      return new RecallAnswerCursor(
          Instant.parse(parts[0]),
          Instant.parse(parts[1]),
          Instant.parse(parts[2]),
          Integer.parseInt(parts[3]));
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("Not a recall answer cursor: " + text, e);
    }
  }
}
