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
  void authoredTokensInOccurrenceOrder_ignoresFileLookingMarkdownHrefs() {
    assertThat(
        WikiLinkMarkdown.authoredTokensInOccurrenceOrder(
            "See [[Folder/Title|wiki]] and [label](/Folder/Title.md)."),
        equalTo(List.of("Folder/Title|wiki")));
  }

  @Test
  void authoredTokensInOccurrenceOrder_skipsImageAndOrdinaryMarkdownHrefs() {
    assertThat(
        WikiLinkMarkdown.authoredTokensInOccurrenceOrder(
            "![alt](/Folder/Title.md) [stay](/n42) [ok](/Folder/Title) [[Wiki]]"),
        equalTo(List.of("Wiki")));
  }

  @Test
  void isWellFormedWholeLinkToken_rejectsFileLookingMarkdownAndBarePath() {
    assertThat(
        WikiLinkMarkdown.isWellFormedWholeLinkToken("[Title](/Folder/Title.md)"), equalTo(false));
    assertThat(WikiLinkMarkdown.isWellFormedWholeLinkToken("/Folder/Title.md"), equalTo(false));
    assertThat(
        WikiLinkMarkdown.isWellFormedWholeLinkToken("[Title](/Folder/Title.md) extra"),
        equalTo(false));
  }

  @Test
  void splitInner_treatsPipeAsSeparator() {
    WikiLinkMarkdown.WikiInnerSplit s = WikiLinkMarkdown.splitInner("Target Note|friendly label");
    assertThat(s.portablePath().format(), equalTo("Target Note"));
    assertThat(s.displayText(), equalTo("friendly label"));
  }

  static Stream<Arguments> escapedWikiInnerExamples() {
    return Stream.of(
        Arguments.of("A|B", "A", "B"),
        Arguments.of("A|B\\|C|D", "A", "B|C|D"),
        Arguments.of("A\\|B\\|C", "A|B|C", "A|B|C"),
        Arguments.of("A\\\\|B", "A\\", "B"),
        Arguments.of("A\\x|B\\x", "A\\x", "B\\x"));
  }

  @ParameterizedTest
  @MethodSource("escapedWikiInnerExamples")
  void splitInner_decodesOnceAndUsesFirstUnescapedPipe(
      String authored, String target, String display) {
    WikiLinkMarkdown.WikiInnerSplit split = WikiLinkMarkdown.splitInner(authored);
    assertThat(split.portablePath().format(), equalTo(target));
    assertThat(split.displayText(), equalTo(display));
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
        WikiLinkMarkdownRewrite.newInnerForFolderRename("  Title  ", "OldFolder", "NewFolder"),
        equalTo("  Title  "));
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
  void replaceWikiLinksMatchingTrimmedInner_preservesYamlScalarSpelling() {
    String markdown =
        """
        ---
        # authored 💡 comment
        relationship: "[[Alpha]]"
        links:
          - "[[Alpha]]"
          - '[[Alpha]]'
        nested:
          relationship: "[[Alpha]]"
        untouched: "[[Other]]"
        ---
        Body [[Alpha]].""";

    String rewritten =
        WikiLinkMarkdownDocumentRewrite.replaceWikiLinksMatchingTrimmedInner(
            markdown, "Alpha", "A\\|B");

    assertThat(
        rewritten,
        equalTo(
            """
            ---
            # authored 💡 comment
            relationship: "[[A\\\\|B]]"
            links:
              - "[[A\\\\|B]]"
              - '[[A\\|B]]'
            nested:
              relationship: "[[Alpha]]"
            untouched: "[[Other]]"
            ---
            Body [[A\\|B]]."""));
    NoteLeadingFrontmatter.Split split = NoteLeadingFrontmatter.split(rewritten).orElseThrow();
    assertThat(
        split.frontmatter().supportedValueStringsInInsertionOrder(),
        equalTo(List.of("[[A\\|B]]", "[[A\\|B]]", "[[A\\|B]]", "[[Other]]")));

    assertThat(
        WikiLinkMarkdownDocumentRewrite.replaceWikiLinksMatchingTrimmedInner(
            rewritten, "A\\|B", "C\\|D"),
        equalTo(
            """
            ---
            # authored 💡 comment
            relationship: "[[C\\\\|D]]"
            links:
              - "[[C\\\\|D]]"
              - '[[C\\|D]]'
            nested:
              relationship: "[[Alpha]]"
            untouched: "[[Other]]"
            ---
            Body [[C\\|D]]."""));
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
