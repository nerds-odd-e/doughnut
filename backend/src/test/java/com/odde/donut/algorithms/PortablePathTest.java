package com.odde.donut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Portable path codec: optional notebook qualifier, note portion (shorthand title or path-shaped
 * spelling), and optional {@code #prop:} encoded property key (ADR 0004). Decoded keys are ordinary
 * property keys ({@link PropertyKeyNaming}, {@code note_property_index.property_key}).
 * Percent-encoding of the property key itself is covered by {@link PropertyKeyPercentEncodingTest}.
 */
class PortablePathTest {

  @Test
  void parse_noteOnlyTargetHasNoPropertySuffix() {
    PortablePath parsed = PortablePath.parse("Moon");
    assertThat(parsed.notePortion(), equalTo("Moon"));
    assertThat(parsed.encodedPropertyKey(), equalTo(Optional.empty()));
    assertThat(parsed.format(), equalTo("Moon"));
  }

  @Test
  void parse_splitsOnFirstPropSeparator() {
    PortablePath parsed = PortablePath.parse("Moon#prop:a%20part%20of");
    assertThat(parsed.notePortion(), equalTo("Moon"));
    assertThat(parsed.encodedPropertyKey(), equalTo(Optional.of("a%20part%20of")));
    assertThat(parsed.format(), equalTo("Moon#prop:a%20part%20of"));
  }

  @Test
  void parse_qualifiedAndPathShapedNoteTargetsKeepRemainderAfterSeparator() {
    assertThat(
        PortablePath.parse("Sky:Moon#prop:a%20part%20of").qualifiedNotePortion(),
        equalTo("Sky:Moon"));
    assertThat(
        PortablePath.parse("/Solar/Moon.md#prop:a%20part%20of").qualifiedNotePortion(),
        equalTo("/Solar/Moon.md"));
  }

  @Test
  void parse_titleContainingLiteralPropSeparatorCannotBeSoleUnqualifiedTarget() {
    PortablePath parsed = PortablePath.parse("Foo#prop:bar");
    assertThat(parsed.notePortion(), equalTo("Foo"));
    assertThat(parsed.encodedPropertyKey(), equalTo(Optional.of("bar")));
  }

  @Test
  void parse_splitsOnFirstMarkerWhenEncodedComponentContainsAnother() {
    PortablePath parsed = PortablePath.parse("Foo#prop:bar#prop:baz");
    assertThat(parsed.notePortion(), equalTo("Foo"));
    assertThat(parsed.encodedPropertyKey(), equalTo(Optional.of("bar#prop:baz")));
  }

  @Test
  void withNoteTitle_preservesEncodedPropertySuffix() {
    PortablePath rewritten = PortablePath.parse("Moon#prop:a%20part%20of").withNoteTitle("Luna");
    assertThat(rewritten.format(), equalTo("Luna#prop:a%20part%20of"));
  }

  @Test
  void format_fromDecodedKeyUsesProductEncoding() {
    PortablePath portablePath =
        new PortablePath(
            Optional.empty(), "Moon", Optional.of(PropertyKeyPercentEncoding.encode("a part of")));
    assertThat(portablePath.format(), equalTo("Moon#prop:a%20part%20of"));
    assertThat(portablePath.decodedPropertyKey(), equalTo(Optional.of("a part of")));
  }

  @Test
  void decodedPropertyKey_emptyWhenEncodedComponentInvalid() {
    assertThat(PortablePath.parse("Moon#prop:%ZZ").decodedPropertyKey(), equalTo(Optional.empty()));
  }

  @Test
  void hasPropertySuffix_isTrueWhenSeparatorPresentEvenIfEncodedKeyEmpty() {
    PortablePath parsed = PortablePath.parse("Moon#prop:");
    assertThat(parsed.hasPropertySuffix(), is(true));
    assertThat(parsed.encodedPropertyKey(), equalTo(Optional.of("")));
    assertThat(parsed.decodedPropertyKey(), equalTo(Optional.empty()));
  }

  @Nested
  class ResolveFocusNotebookFallback {

    @Test
    void resolve_qualifiedTokenUsesItsQualifierIgnoringFocusNotebook() {
      assertThat(
          PortablePath.parse("Sky:Moon").resolve("FocusNb"),
          equalTo(Optional.of(new PortablePath.Resolved("Sky", "Moon"))));
    }

    @Test
    void resolve_unqualifiedTokenFallsBackToFocusNotebook() {
      assertThat(
          PortablePath.parse("Moon").resolve("FocusNb"),
          equalTo(Optional.of(new PortablePath.Resolved("FocusNb", "Moon"))));
    }

    @Test
    void resolve_unqualifiedTokenWithNoFocusNotebookResolvesToNothing() {
      assertThat(PortablePath.parse("Moon").resolve(null), equalTo(Optional.empty()));
    }
  }

  /**
   * {@link PathShapedTarget#tryParse} rejects any target containing {@code :}. Qualified {@code
   * Notebook:Title} rewrites therefore never go through path-shaped parsing, and they replace the
   * entire remainder after the notebook colon — silently dropping a suffix. {@code #prop:} contains
   * {@code :}; the codec must split that suffix off before path/qualified parsing so this trap is
   * not inherited.
   */
  @Nested
  class PathShapedTargetColonRejection {

    @Test
    void tryParse_returnsEmptyForQualifiedNotebookTitle() {
      assertThat(PathShapedTarget.tryParse("Notebook:Title"), equalTo(Optional.empty()));
    }

    @Test
    void tryParse_returnsEmptyForPathShapedTargetWithColonSuffix() {
      assertThat(PathShapedTarget.tryParse("Folder/Title:suffix"), equalTo(Optional.empty()));
    }

    @Test
    void tryParse_returnsEmptyWhenTargetContainsPropSeparatorColon() {
      assertThat(PathShapedTarget.tryParse("Folder/Title#prop:key"), equalTo(Optional.empty()));
    }

    @Test
    void replaceNoteTitle_qualifiedRewriteDropsNonPropertySuffix() {
      assertThat(
          PortablePath.replaceNoteTitle("MyNb:OldTitle#heading", "NewTitle"),
          equalTo("MyNb:NewTitle"));
    }
  }

  @Nested
  class NoteOnlyRewritesUnchanged {

    @Test
    void replaceNoteTitle_unqualifiedTitle() {
      assertThat(PortablePath.replaceNoteTitle("OldTitle", "NewTitle"), equalTo("NewTitle"));
    }

    @Test
    void replaceNoteTitle_qualifiedNotebookTitle() {
      assertThat(
          PortablePath.replaceNoteTitle("MyNb:OldTitle", "NewTitle"), equalTo("MyNb:NewTitle"));
    }

    @Test
    void replaceNoteTitle_pathShapedKeepsFolderAndMarkdownSuffix() {
      assertThat(PortablePath.replaceNoteTitle("/Folder/Old.md", "New"), equalTo("/Folder/New.md"));
    }

    @Test
    void replaceFolderName_rewritesOneFolderSegment() {
      assertThat(
          PortablePath.replaceFolderName("Parent/OldFolder/Title", "OldFolder", "NewFolder"),
          equalTo("Parent/NewFolder/Title"));
    }

    @Test
    void replaceFolderName_leavesUnqualifiedTitleUnchanged() {
      assertThat(
          PortablePath.replaceFolderName("Title", "OldFolder", "NewFolder"), equalTo("Title"));
    }

    @Test
    void replaceNotebookName_qualifiesUnqualifiedTitle() {
      assertThat(PortablePath.replaceNotebookName("Title", "NewNb"), equalTo("NewNb:Title"));
    }

    @Test
    void replaceNotebookName_replacesQualifiedNotebookPrefix() {
      assertThat(PortablePath.replaceNotebookName("OldNb:Title", "NewNb"), equalTo("NewNb:Title"));
    }
  }

  @Nested
  class PropertySuffixPreservedOnNoteTargetRewrite {

    @Test
    void replaceNoteTitle_preservesEncodedPropertySuffixOnUnqualifiedTitle() {
      assertThat(
          PortablePath.replaceNoteTitle("Moon#prop:a%20part%20of", "Luna"),
          equalTo("Luna#prop:a%20part%20of"));
    }

    @Test
    void replaceNoteTitle_preservesEncodedPropertySuffixOnQualifiedTitle() {
      assertThat(
          PortablePath.replaceNoteTitle("MyNb:Moon#prop:a%20part%20of", "Luna"),
          equalTo("MyNb:Luna#prop:a%20part%20of"));
    }

    @Test
    void replaceNoteTitle_preservesEncodedPropertySuffixOnPathShapedTarget() {
      assertThat(
          PortablePath.replaceNoteTitle("/Solar/Moon.md#prop:a%20part%20of", "Luna"),
          equalTo("/Solar/Luna.md#prop:a%20part%20of"));
    }

    @Test
    void replaceFolderName_preservesEncodedPropertySuffix() {
      assertThat(
          PortablePath.replaceFolderName("Solar/Moon#prop:a%20part%20of", "Solar", "Helios"),
          equalTo("Helios/Moon#prop:a%20part%20of"));
    }

    @Test
    void replaceNotebookName_preservesEncodedPropertySuffixOnUnqualifiedTitle() {
      assertThat(
          PortablePath.replaceNotebookName("Moon#prop:a%20part%20of", "Sky"),
          equalTo("Sky:Moon#prop:a%20part%20of"));
    }

    @Test
    void replaceNotebookName_preservesEncodedPropertySuffixOnQualifiedTitle() {
      assertThat(
          PortablePath.replaceNotebookName("OldNb:Moon#prop:a%20part%20of", "Sky"),
          equalTo("Sky:Moon#prop:a%20part%20of"));
    }
  }
}
