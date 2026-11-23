package com.odde.donut.entities;

import java.sql.Timestamp;
import java.util.regex.Pattern;

/** The focus note a recall question was generated from, as read when preparing the request. */
public record FocusNoteInput(Integer noteId, Timestamp readAt, String fingerprint) {
  private static final Pattern SHA_256_HEX = Pattern.compile("[0-9a-f]{64}");

  public FocusNoteInput {
    if (!isValidFingerprint(fingerprint)) {
      throw new IllegalArgumentException("Focus note input requires a SHA-256 fingerprint");
    }
  }

  public static boolean isValidFingerprint(String fingerprint) {
    return fingerprint != null && SHA_256_HEX.matcher(fingerprint).matches();
  }

  public boolean wasReadAfterLastUpdateOf(Note focus) {
    return focus.getId().equals(noteId) && readAt != null && focus.getUpdatedAt().before(readAt);
  }
}
