package com.odde.donut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Authored wiki/path-Markdown target codec: note target plus optional {@code #prop:} encoded key
 * (ADR 0004). Decoded keys are ordinary property keys ({@link PropertyKeyNaming}, {@code
 * note_property_index.property_key}).
 *
 * <p>Encode pairs below are the shared fixture table for the TypeScript codec (same input/output
 * examples: spaces, Unicode, {@code /}, {@code %}, {@code |}, {@code ]}, {@code ?}, {@code #},
 * mixed-case, unreserved).
 */
class WikiLinkAuthoredTargetTest {

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
  void encodePropertyKey_usesRfc3986UnreservedAndUppercaseHex(String yamlKey, String encoded) {
    assertThat(WikiLinkAuthoredTarget.encodePropertyKey(yamlKey), equalTo(encoded));
  }

  @ParameterizedTest
  @MethodSource("encodedPropertyKeyPairs")
  void decodePropertyKey_roundTripsProductEncoding(String yamlKey, String encoded) {
    assertThat(WikiLinkAuthoredTarget.decodePropertyKey(encoded), equalTo(Optional.of(yamlKey)));
  }

  @Test
  void decodePropertyKey_acceptsLowercaseHex() {
    assertThat(WikiLinkAuthoredTarget.decodePropertyKey("a%2fb"), equalTo(Optional.of("a/b")));
  }

  @Test
  void decodePropertyKey_rejectsInvalidEscape() {
    assertThat(WikiLinkAuthoredTarget.decodePropertyKey("%"), equalTo(Optional.empty()));
    assertThat(WikiLinkAuthoredTarget.decodePropertyKey("%2"), equalTo(Optional.empty()));
    assertThat(WikiLinkAuthoredTarget.decodePropertyKey("%ZZ"), equalTo(Optional.empty()));
  }

  @Test
  void decodePropertyKey_rejectsInvalidUtf8() {
    assertThat(WikiLinkAuthoredTarget.decodePropertyKey("%80"), equalTo(Optional.empty()));
  }

  @Test
  void decodePropertyKey_rejectsEmptyEncodedComponent() {
    assertThat(WikiLinkAuthoredTarget.decodePropertyKey(""), equalTo(Optional.empty()));
  }

  @Test
  void parse_noteOnlyTargetHasNoPropertySuffix() {
    WikiLinkAuthoredTarget parsed = WikiLinkAuthoredTarget.parse("Moon");
    assertThat(parsed.noteTarget(), equalTo("Moon"));
    assertThat(parsed.encodedPropertyKey(), nullValue());
    assertThat(parsed.format(), equalTo("Moon"));
  }

  @Test
  void parse_splitsOnFirstPropSeparator() {
    WikiLinkAuthoredTarget parsed = WikiLinkAuthoredTarget.parse("Moon#prop:a%20part%20of");
    assertThat(parsed.noteTarget(), equalTo("Moon"));
    assertThat(parsed.encodedPropertyKey(), equalTo("a%20part%20of"));
    assertThat(parsed.format(), equalTo("Moon#prop:a%20part%20of"));
  }

  @Test
  void parse_qualifiedAndPathShapedNoteTargetsKeepRemainderAfterSeparator() {
    assertThat(
        WikiLinkAuthoredTarget.parse("Sky:Moon#prop:a%20part%20of").noteTarget(),
        equalTo("Sky:Moon"));
    assertThat(
        WikiLinkAuthoredTarget.parse("/Solar/Moon.md#prop:a%20part%20of").noteTarget(),
        equalTo("/Solar/Moon.md"));
  }

  @Test
  void parse_titleContainingLiteralPropSeparatorCannotBeSoleUnqualifiedTarget() {
    WikiLinkAuthoredTarget parsed = WikiLinkAuthoredTarget.parse("Foo#prop:bar");
    assertThat(parsed.noteTarget(), equalTo("Foo"));
    assertThat(parsed.encodedPropertyKey(), equalTo("bar"));
  }

  @Test
  void parse_splitsOnFirstMarkerWhenEncodedComponentContainsAnother() {
    WikiLinkAuthoredTarget parsed = WikiLinkAuthoredTarget.parse("Foo#prop:bar#prop:baz");
    assertThat(parsed.noteTarget(), equalTo("Foo"));
    assertThat(parsed.encodedPropertyKey(), equalTo("bar#prop:baz"));
  }

  @Test
  void withNoteTarget_preservesEncodedPropertySuffix() {
    WikiLinkAuthoredTarget rewritten =
        WikiLinkAuthoredTarget.parse("Moon#prop:a%20part%20of").withNoteTarget("Luna");
    assertThat(rewritten.format(), equalTo("Luna#prop:a%20part%20of"));
  }

  @Test
  void format_fromDecodedKeyUsesProductEncoding() {
    WikiLinkAuthoredTarget target =
        new WikiLinkAuthoredTarget("Moon", WikiLinkAuthoredTarget.encodePropertyKey("a part of"));
    assertThat(target.format(), equalTo("Moon#prop:a%20part%20of"));
    assertThat(target.decodedPropertyKey(), equalTo(Optional.of("a part of")));
  }

  @Test
  void decodedPropertyKey_emptyWhenEncodedComponentInvalid() {
    assertThat(
        WikiLinkAuthoredTarget.parse("Moon#prop:%ZZ").decodedPropertyKey(),
        equalTo(Optional.empty()));
  }

  @Test
  void hasPropertySuffix_isTrueWhenSeparatorPresentEvenIfEncodedKeyEmpty() {
    WikiLinkAuthoredTarget parsed = WikiLinkAuthoredTarget.parse("Moon#prop:");
    assertThat(parsed.hasPropertySuffix(), is(true));
    assertThat(parsed.encodedPropertyKey(), equalTo(""));
    assertThat(parsed.decodedPropertyKey(), equalTo(Optional.empty()));
  }
}
