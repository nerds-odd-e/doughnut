package com.odde.donut.algorithms;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Authored wiki or path-Markdown link target: note target plus optional {@code #prop:} encoded
 * property key (ADR 0004). The decoded key is the same YAML property-key string used by {@link
 * PropertyKeyNaming} and {@code note_property_index.property_key}.
 */
public record WikiLinkAuthoredTarget(String noteTarget, String encodedPropertyKey) {

  private static final String PROPERTY_SEPARATOR = "#prop:";

  private static final boolean[] UNRESERVED = new boolean[128];

  static {
    for (char c = 'A'; c <= 'Z'; c++) {
      UNRESERVED[c] = true;
    }
    for (char c = 'a'; c <= 'z'; c++) {
      UNRESERVED[c] = true;
    }
    for (char c = '0'; c <= '9'; c++) {
      UNRESERVED[c] = true;
    }
    UNRESERVED['-'] = true;
    UNRESERVED['.'] = true;
    UNRESERVED['_'] = true;
    UNRESERVED['~'] = true;
  }

  public static WikiLinkAuthoredTarget parse(String target) {
    if (target == null) {
      return new WikiLinkAuthoredTarget("", null);
    }
    int separator = target.indexOf(PROPERTY_SEPARATOR);
    if (separator < 0) {
      return new WikiLinkAuthoredTarget(target, null);
    }
    return new WikiLinkAuthoredTarget(
        target.substring(0, separator), target.substring(separator + PROPERTY_SEPARATOR.length()));
  }

  public String format() {
    if (encodedPropertyKey == null) {
      return noteTarget;
    }
    return noteTarget + PROPERTY_SEPARATOR + encodedPropertyKey;
  }

  public boolean hasPropertySuffix() {
    return encodedPropertyKey != null;
  }

  public WikiLinkAuthoredTarget withNoteTarget(String newNoteTarget) {
    return new WikiLinkAuthoredTarget(newNoteTarget, encodedPropertyKey);
  }

  public WikiLinkAuthoredTarget mapNoteTarget(UnaryOperator<String> noteTargetTransform) {
    return withNoteTarget(noteTargetTransform.apply(noteTarget));
  }

  public Optional<String> decodedPropertyKey() {
    return decodePropertyKey(encodedPropertyKey);
  }

  /**
   * Encodes a YAML property key as one {@code #prop:} component: RFC 3986 unreserved characters
   * stay literal; every other UTF-8 byte is uppercase {@code %HH}.
   */
  public static String encodePropertyKey(String yamlKey) {
    byte[] bytes = yamlKey.getBytes(StandardCharsets.UTF_8);
    StringBuilder out = new StringBuilder(bytes.length);
    for (byte raw : bytes) {
      int unsigned = raw & 0xFF;
      if (unsigned < UNRESERVED.length && UNRESERVED[unsigned]) {
        out.append((char) unsigned);
      } else {
        out.append('%');
        out.append(hexUpper(unsigned >> 4));
        out.append(hexUpper(unsigned & 0xF));
      }
    }
    return out.toString();
  }

  /**
   * Decodes one {@code #prop:} component. Product output uses uppercase hex; either hex case is
   * accepted. Invalid escape, empty component, or invalid UTF-8 yields empty.
   */
  public static Optional<String> decodePropertyKey(String encoded) {
    if (encoded == null || encoded.isEmpty()) {
      return Optional.empty();
    }
    ByteArrayOutputStream buf = new ByteArrayOutputStream(encoded.length());
    for (int i = 0; i < encoded.length(); ) {
      char c = encoded.charAt(i);
      if (c == '%') {
        if (i + 2 >= encoded.length()) {
          return Optional.empty();
        }
        int hi = hexValue(encoded.charAt(i + 1));
        int lo = hexValue(encoded.charAt(i + 2));
        if (hi < 0 || lo < 0) {
          return Optional.empty();
        }
        buf.write((hi << 4) | lo);
        i += 3;
      } else {
        byte[] charBytes = Character.toString(c).getBytes(StandardCharsets.UTF_8);
        buf.write(charBytes, 0, charBytes.length);
        i++;
      }
    }
    return decodeUtf8(buf.toByteArray());
  }

  private static Optional<String> decodeUtf8(byte[] bytes) {
    CharsetDecoder decoder =
        StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
    try {
      return Optional.of(decoder.decode(ByteBuffer.wrap(bytes)).toString());
    } catch (CharacterCodingException _) {
      return Optional.empty();
    }
  }

  private static char hexUpper(int nibble) {
    return "0123456789ABCDEF".charAt(nibble);
  }

  private static int hexValue(char c) {
    if (c >= '0' && c <= '9') {
      return c - '0';
    }
    if (c >= 'A' && c <= 'F') {
      return c - 'A' + 10;
    }
    if (c >= 'a' && c <= 'f') {
      return c - 'a' + 10;
    }
    return -1;
  }
}
