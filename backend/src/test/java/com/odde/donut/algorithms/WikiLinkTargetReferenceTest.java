package com.odde.donut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WikiLinkTargetReferenceTest {

  /**
   * {@link WikiLinkTargetReference.PathShapedTarget#tryParse} rejects any target containing {@code
   * :}. Qualified {@code Notebook:Title} rewrites therefore never go through path-shaped parsing,
   * and they replace the entire remainder after the notebook colon — silently dropping a suffix.
   * {@code #prop:} contains {@code :}; the authored-target codec must split that suffix off before
   * path/qualified parsing so this trap is not inherited.
   */
  @Nested
  class PathShapedTargetColonRejection {

    @Test
    void tryParse_returnsEmptyForQualifiedNotebookTitle() {
      assertThat(
          WikiLinkTargetReference.PathShapedTarget.tryParse("Notebook:Title"),
          equalTo(Optional.empty()));
    }

    @Test
    void tryParse_returnsEmptyForPathShapedTargetWithColonSuffix() {
      assertThat(
          WikiLinkTargetReference.PathShapedTarget.tryParse("Folder/Title:suffix"),
          equalTo(Optional.empty()));
    }

    @Test
    void tryParse_returnsEmptyWhenTargetContainsPropSeparatorColon() {
      assertThat(
          WikiLinkTargetReference.PathShapedTarget.tryParse("Folder/Title#prop:key"),
          equalTo(Optional.empty()));
    }

    @Test
    void replaceNoteTitle_qualifiedRewriteDropsNonPropertySuffix() {
      assertThat(
          WikiLinkTargetReference.replaceNoteTitle("MyNb:OldTitle#heading", "NewTitle"),
          equalTo("MyNb:NewTitle"));
    }
  }

  @Nested
  class NoteOnlyRewritesUnchanged {

    @Test
    void replaceNoteTitle_unqualifiedTitle() {
      assertThat(
          WikiLinkTargetReference.replaceNoteTitle("OldTitle", "NewTitle"), equalTo("NewTitle"));
    }

    @Test
    void replaceNoteTitle_qualifiedNotebookTitle() {
      assertThat(
          WikiLinkTargetReference.replaceNoteTitle("MyNb:OldTitle", "NewTitle"),
          equalTo("MyNb:NewTitle"));
    }

    @Test
    void replaceNoteTitle_pathShapedKeepsFolderAndMarkdownSuffix() {
      assertThat(
          WikiLinkTargetReference.replaceNoteTitle("/Folder/Old.md", "New"),
          equalTo("/Folder/New.md"));
    }

    @Test
    void replaceFolderName_rewritesOneFolderSegment() {
      assertThat(
          WikiLinkTargetReference.replaceFolderName(
              "Parent/OldFolder/Title", "OldFolder", "NewFolder"),
          equalTo("Parent/NewFolder/Title"));
    }

    @Test
    void replaceFolderName_leavesUnqualifiedTitleUnchanged() {
      assertThat(
          WikiLinkTargetReference.replaceFolderName("Title", "OldFolder", "NewFolder"),
          equalTo("Title"));
    }

    @Test
    void replaceNotebookName_qualifiesUnqualifiedTitle() {
      assertThat(
          WikiLinkTargetReference.replaceNotebookName("Title", "NewNb"), equalTo("NewNb:Title"));
    }

    @Test
    void replaceNotebookName_replacesQualifiedNotebookPrefix() {
      assertThat(
          WikiLinkTargetReference.replaceNotebookName("OldNb:Title", "NewNb"),
          equalTo("NewNb:Title"));
    }
  }

  @Nested
  class PropertySuffixPreservedOnNoteTargetRewrite {

    @Test
    void replaceNoteTitle_preservesEncodedPropertySuffixOnUnqualifiedTitle() {
      assertThat(
          WikiLinkTargetReference.replaceNoteTitle("Moon#prop:a%20part%20of", "Luna"),
          equalTo("Luna#prop:a%20part%20of"));
    }

    @Test
    void replaceNoteTitle_preservesEncodedPropertySuffixOnQualifiedTitle() {
      assertThat(
          WikiLinkTargetReference.replaceNoteTitle("MyNb:Moon#prop:a%20part%20of", "Luna"),
          equalTo("MyNb:Luna#prop:a%20part%20of"));
    }

    @Test
    void replaceNoteTitle_preservesEncodedPropertySuffixOnPathShapedTarget() {
      assertThat(
          WikiLinkTargetReference.replaceNoteTitle("/Solar/Moon.md#prop:a%20part%20of", "Luna"),
          equalTo("/Solar/Luna.md#prop:a%20part%20of"));
    }

    @Test
    void replaceFolderName_preservesEncodedPropertySuffix() {
      assertThat(
          WikiLinkTargetReference.replaceFolderName(
              "Solar/Moon#prop:a%20part%20of", "Solar", "Helios"),
          equalTo("Helios/Moon#prop:a%20part%20of"));
    }

    @Test
    void replaceNotebookName_preservesEncodedPropertySuffixOnUnqualifiedTitle() {
      assertThat(
          WikiLinkTargetReference.replaceNotebookName("Moon#prop:a%20part%20of", "Sky"),
          equalTo("Sky:Moon#prop:a%20part%20of"));
    }

    @Test
    void replaceNotebookName_preservesEncodedPropertySuffixOnQualifiedTitle() {
      assertThat(
          WikiLinkTargetReference.replaceNotebookName("OldNb:Moon#prop:a%20part%20of", "Sky"),
          equalTo("Sky:Moon#prop:a%20part%20of"));
    }
  }
}
