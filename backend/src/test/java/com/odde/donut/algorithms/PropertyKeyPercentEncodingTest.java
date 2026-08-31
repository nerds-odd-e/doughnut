package com.odde.donut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * RFC 3986 percent-encoding for the YAML property key carried in a {@link PortablePath}'s {@code
 * #prop:} suffix (ADR 0004).
 *
 * <p>Encode pairs below are the shared fixture table for the TypeScript codec (same input/output
 * examples: spaces, Unicode, {@code /}, {@code %}, {@code |}, {@code ]}, {@code ?}, {@code #},
 * mixed-case, unreserved).
 */
class PropertyKeyPercentEncodingTest {

  static Stream<Arguments> encodedPropertyKeyPairs() {
    return Stream.of(
        Arguments.of("a part of", "a%20part%20of"),
        Arguments.of("月", "%E6%9C%88"),
        Arguments.of("a/b", "a%2Fb"),
        Arguments.of("100%", "100%25"),
        Arguments.of("a|b", "a%7Cb"),
        Arguments.of("a]b", "a%5Db"),
        Arguments.of("a?b", "a%3Fb"),
        Arguments.of("a#b", "a%23b"),
        Arguments.of("WikiData", "WikiData"),
        Arguments.of("Az09-._~", "Az09-._~"));
  }

  @ParameterizedTest
  @MethodSource("encodedPropertyKeyPairs")
  void encode_usesRfc3986UnreservedAndUppercaseHex(String yamlKey, String encoded) {
    assertThat(PropertyKeyPercentEncoding.encode(yamlKey), equalTo(encoded));
  }

  @ParameterizedTest
  @MethodSource("encodedPropertyKeyPairs")
  void decode_roundTripsProductEncoding(String yamlKey, String encoded) {
    assertThat(PropertyKeyPercentEncoding.decode(encoded), equalTo(Optional.of(yamlKey)));
  }

  @Test
  void decode_acceptsLowercaseHex() {
    assertThat(PropertyKeyPercentEncoding.decode("a%2fb"), equalTo(Optional.of("a/b")));
  }

  @Test
  void decode_rejectsInvalidEscape() {
    assertThat(PropertyKeyPercentEncoding.decode("%"), equalTo(Optional.empty()));
    assertThat(PropertyKeyPercentEncoding.decode("%2"), equalTo(Optional.empty()));
    assertThat(PropertyKeyPercentEncoding.decode("%ZZ"), equalTo(Optional.empty()));
  }

  @Test
  void decode_rejectsInvalidUtf8() {
    assertThat(PropertyKeyPercentEncoding.decode("%80"), equalTo(Optional.empty()));
  }

  @Test
  void decode_rejectsEmptyEncodedComponent() {
    assertThat(PropertyKeyPercentEncoding.decode(""), equalTo(Optional.empty()));
  }
}
