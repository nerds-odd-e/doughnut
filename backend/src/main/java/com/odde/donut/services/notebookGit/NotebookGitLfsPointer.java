package com.odde.donut.services.notebookGit;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Standard Git LFS v1 pointer classification for accepted Git attachment content. An empty byte
 * array is the standard empty-file representation (passed through LFS unchanged); it is not a
 * textual pointer. Detecting pointer shape does not by itself enable LFS for a notebook: raw
 * bindings keep pointer-looking bytes as legacy payload.
 */
public final class NotebookGitLfsPointer {

  public static final String VERSION = "https://git-lfs.github.com/spec/v1";
  private static final int MAX_POINTER_BYTES = 1024;
  private static final Pattern POINTER =
      Pattern.compile(
          "version "
              + Pattern.quote(VERSION)
              + "\n"
              + "oid sha256:([0-9a-f]{64})\n"
              + "size (0|[1-9][0-9]*)\n");

  private NotebookGitLfsPointer() {}

  /** True when {@code bytes} is the standard empty-file representation. */
  public static boolean isEmptyFile(byte[] bytes) {
    return bytes != null && bytes.length == 0;
  }

  /**
   * Parses a canonical Git LFS v1 pointer. Empty files are not pointers here; use {@link
   * #isEmptyFile(byte[])}.
   */
  public static Optional<Parsed> parse(byte[] bytes) {
    if (bytes == null || bytes.length == 0 || bytes.length >= MAX_POINTER_BYTES) {
      return Optional.empty();
    }
    for (byte b : bytes) {
      int unsigned = b & 0xff;
      if (unsigned < 0x20 && unsigned != 0x0A || unsigned > 0x7E) {
        return Optional.empty();
      }
    }
    String text = new String(bytes, StandardCharsets.US_ASCII);
    Matcher matcher = POINTER.matcher(text);
    if (!matcher.matches()) {
      return Optional.empty();
    }
    return Optional.of(new Parsed(matcher.group(1), Long.parseLong(matcher.group(2))));
  }

  public static boolean isPointer(byte[] bytes) {
    return parse(bytes).isPresent();
  }

  /** Canonical pointer bytes for the given SHA-256 hex digest and payload size. */
  public static byte[] format(String sha256Hex, long size) {
    return ("version " + VERSION + "\noid sha256:" + sha256Hex + "\nsize " + size + "\n")
        .getBytes(StandardCharsets.US_ASCII);
  }

  public record Parsed(String sha256Hex, long size) {}
}
