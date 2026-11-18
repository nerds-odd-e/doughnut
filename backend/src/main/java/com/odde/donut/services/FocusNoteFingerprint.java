package com.odde.donut.services;

import com.odde.donut.entities.Note;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Identifies the authored focus used to generate a recall question. */
public final class FocusNoteFingerprint {
  private FocusNoteFingerprint() {}

  public static String of(Note note) {
    MessageDigest digest = sha256();
    updateInt(digest, note.getId());
    updateText(digest, note.getTitle());
    updateText(digest, note.getContent());
    return HexFormat.of().formatHex(digest.digest());
  }

  private static void updateText(MessageDigest digest, String text) {
    if (text == null) {
      updateInt(digest, -1);
      return;
    }
    byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);
    updateInt(digest, utf8.length);
    digest.update(utf8);
  }

  private static void updateInt(MessageDigest digest, int value) {
    digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(value).array());
  }

  private static MessageDigest sha256() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
