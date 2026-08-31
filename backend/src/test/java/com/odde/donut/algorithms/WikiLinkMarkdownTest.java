package com.odde.donut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class WikiLinkMarkdownTest {

  @Test
  void authoredTokensInOccurrenceOrder_includesPathMarkdownInDocumentOrderWithWiki() {
    assertThat(
        WikiLinkMarkdown.authoredTokensInOccurrenceOrder(
            "See [[Folder/Title|wiki]] and [label](/Folder/Title.md)."),
        equalTo(List.of("Folder/Title|wiki", "[label](/Folder/Title.md)")));
  }

  @Test
  void authoredTokensInOccurrenceOrder_skipsImageMarkdownAndNoteShowHrefs() {
    assertThat(
        WikiLinkMarkdown.authoredTokensInOccurrenceOrder(
            "![alt](/Folder/Title.md) [stay](/n42) [ok](/Folder/Title)"),
        equalTo(List.of("[ok](/Folder/Title)")));
  }

  @Test
  void isWellFormedWholeLinkToken_acceptsPathMarkdown() {
    assertThat(
        WikiLinkMarkdown.isWellFormedWholeLinkToken("[Title](/Folder/Title.md)"), equalTo(true));
  }

  @Test
  void isWellFormedWholeLinkToken_rejectsBarePathAndMixedJunk() {
    assertThat(WikiLinkMarkdown.isWellFormedWholeLinkToken("/Folder/Title.md"), equalTo(false));
    assertThat(
        WikiLinkMarkdown.isWellFormedWholeLinkToken("[Title](/Folder/Title.md) extra"),
        equalTo(false));
  }

  @Test
  void splitAuthoredToken_readsPathMarkdownHrefAsTarget() {
    WikiLinkMarkdown.WikiInnerSplit s =
        WikiLinkMarkdown.splitAuthoredToken("[label](/Folder/Title.md)");
    assertThat(s.portablePath().format(), equalTo("/Folder/Title.md"));
    assertThat(s.displayText(), equalTo("label"));
  }

  @Test
  void splitInner_treatsPipeAsSeparator() {
    WikiLinkMarkdown.WikiInnerSplit s = WikiLinkMarkdown.splitInner("Target Note|friendly label");
    assertThat(s.portablePath().format(), equalTo("Target Note"));
    assertThat(s.displayText(), equalTo("friendly label"));
  }

  @Test
  void splitInner_emptyRightSideActsAsNoPipe() {
    WikiLinkMarkdown.WikiInnerSplit s = WikiLinkMarkdown.splitInner("Alpha|");
    assertThat(s.portablePath().format(), equalTo("Alpha"));
    assertThat(s.displayText(), equalTo("Alpha"));
  }

  @Test
  void newInnerForFolderRename_rewritesOneFolderSegmentNotTitle() {
    assertThat(
        WikiLinkMarkdownRewrite.newInnerForFolderRename(
            "Parent/OldFolder/OldFolder", "OldFolder", "NewFolder"),
        equalTo("Parent/NewFolder/OldFolder"));
  }

  @Test
  void newInnerForFolderRename_leavesUnqualifiedTitleUnchanged() {
    assertThat(
        WikiLinkMarkdownRewrite.newInnerForFolderRename("Title", "OldFolder", "NewFolder"),
        equalTo("Title"));
  }

  @Test
  void newInnerForQualifyUnqualifiedOutgoingLink_preservesCustomDisplay() {
    assertThat(
        WikiLinkMarkdownRewrite.newInnerForQualifyUnqualifiedOutgoingLink(
            "Target|friendly label", "Source Notebook"),
        equalTo("Source Notebook:Target|friendly label"));
  }

  @Test
  void newInnerForQualifyUnqualifiedOutgoingLink_emptyPipeUsesTargetAsDisplay() {
    assertThat(
        WikiLinkMarkdownRewrite.newInnerForQualifyUnqualifiedOutgoingLink(
            "Target|", "Source Notebook"),
        equalTo("Source Notebook:Target|Target"));
  }

  @Test
  void newInnerForQualifyUnqualifiedOutgoingLink_keepsAlreadyQualifiedDisplayLink() {
    assertThat(
        WikiLinkMarkdownRewrite.newInnerForQualifyUnqualifiedOutgoingLink(
            "Other Notebook:Target|friendly label", "Source Notebook"),
        equalTo("Other Notebook:Target|friendly label"));
  }

  @Test
  void newInnerForQualifyUnqualifiedOutgoingLink_keepsBlankInner() {
    assertThat(
        WikiLinkMarkdownRewrite.newInnerForQualifyUnqualifiedOutgoingLink("   ", "Source Notebook"),
        equalTo("   "));
  }

  @Test
  void replaceWikiLinksMatchingTrimmedInner_matchesWhitespaceInsideBrackets() {
    assertThat(
        WikiLinkMarkdownDocumentRewrite.replaceWikiLinksMatchingTrimmedInner(
            "see [[  Old  ]] end", "Old", "NewTitle"),
        equalTo("see [[NewTitle]] end"));
  }

  @Test
  void newInnerForKeepNotebookMove_qualifiesUnqualifiedAndKeepsVisibleText() {
    assertThat(
        WikiLinkMarkdownRewrite.newInnerForKeepNotebookMove("Title", "NewNb"),
        equalTo("NewNb:Title|Title"));
  }

  @Test
  void newInnerForKeepNotebookMove_replacesQualifiedPrefixAndKeepsVisibleText() {
    assertThat(
        WikiLinkMarkdownRewrite.newInnerForKeepNotebookMove("OldNb:Title", "NewNb"),
        equalTo("NewNb:Title|OldNb:Title"));
  }

  @Test
  void newInnerForFolderRename_preservesEncodedPropertySuffix() {
    assertThat(
        WikiLinkMarkdownRewrite.newInnerForFolderRename(
            "Solar/Moon#prop:a%20part%20of", "Solar", "Helios"),
        equalTo("Helios/Moon#prop:a%20part%20of"));
  }

  @Test
  void newInnerForKeepNotebookMove_preservesEncodedPropertySuffixAndDisplay() {
    assertThat(
        WikiLinkMarkdownRewrite.newInnerForKeepNotebookMove("Moon#prop:a%20part%20of", "Sky"),
        equalTo("Sky:Moon#prop:a%20part%20of|Moon#prop:a%20part%20of"));
  }

  @Test
  void newInnerForQualifyUnqualifiedOutgoingLink_preservesEncodedPropertySuffix() {
    assertThat(
        WikiLinkMarkdownRewrite.newInnerForQualifyUnqualifiedOutgoingLink(
            "Moon#prop:a%20part%20of", "Sky"),
        equalTo("Sky:Moon#prop:a%20part%20of|Moon#prop:a%20part%20of"));
  }

  static Stream<Arguments> osInvalidSanitizationKeepsEncodedPropertySuffix() {
    return Stream.of(
        Arguments.of("[[Moon#prop:a%20part%20of]]", "[[Moon#prop:a%20part%20of]]"),
        Arguments.of("[[Sky:Moon#prop:a%20part%20of]]", "[[Sky:Moon#prop:a%20part%20of]]"),
        Arguments.of("[[Folder/Title#prop:a%20part%20of]]", "[[Folder/Title#prop:a%20part%20of]]"),
        Arguments.of(
            "[label](/Solar/Moon.md*#prop:a%20part%20of)",
            "[label](/Solar/Moon.md＊#prop:a%20part%20of)"),
        Arguments.of("[[Sky:Moon*#prop:a%20part%20of]]", "[[Sky:Moon＊#prop:a%20part%20of]]"),
        Arguments.of(
            "[[Folder/Title*#prop:a%20part%20of]]", "[[Folder/Title＊#prop:a%20part%20of]]"));
  }

  @ParameterizedTest
  @MethodSource("osInvalidSanitizationKeepsEncodedPropertySuffix")
  void replaceOsInvalidCharsInAuthoredTokens_preservesEncodedPropertySuffix(
      String markdown, String expected) {
    assertThat(
        WikiLinkMarkdownDocumentRewrite.replaceOsInvalidCharsInAuthoredTokens(markdown),
        equalTo(expected));
  }
}
