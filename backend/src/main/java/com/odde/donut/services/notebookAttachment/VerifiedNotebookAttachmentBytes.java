package com.odde.donut.services.notebookAttachment;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Counts and SHA-256-hashes bytes; accepts them only when both match the claim. */
public final class VerifiedNotebookAttachmentBytes {

  private VerifiedNotebookAttachmentBytes() {}

  /** True when {@code bytes} match the claimed size and SHA-256 hex digest. */
  public static boolean matchesClaim(byte[] bytes, String sha256Hex, long size) {
    if (bytes == null || bytes.length != size) {
      return false;
    }
    return sha256Hex(bytes).equalsIgnoreCase(sha256Hex);
  }

  static byte[] readMatchingOrNull(InputStream content, String sha256Hex, long size)
      throws IOException {
    byte[] bytes = content.readAllBytes();
    return matchesClaim(bytes, sha256Hex, size) ? bytes : null;
  }

  private static String sha256Hex(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
