package com.odde.donut.services.notebookAttachment;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Counts and SHA-256-hashes a stream; returns bytes only when both match the claim. */
final class VerifiedNotebookAttachmentBytes {

  private VerifiedNotebookAttachmentBytes() {}

  static byte[] readMatchingOrNull(InputStream content, String sha256Hex, long size)
      throws IOException {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
    byte[] bytes;
    try (DigestInputStream in = new DigestInputStream(content, digest)) {
      bytes = in.readAllBytes();
    }
    if (bytes.length != size) {
      return null;
    }
    String actual = HexFormat.of().formatHex(digest.digest());
    if (!actual.equalsIgnoreCase(sha256Hex)) {
      return null;
    }
    return bytes;
  }
}
