package com.odde.donut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import org.junit.jupiter.api.Test;

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
    assertThat(s.target(), equalTo("/Folder/Title.md"));
    assertThat(s.display(), equalTo("label"));
  }

  @Test
  void splitInner_treatsPipeAsSeparator() {
    WikiLinkMarkdown.WikiInnerSplit s = WikiLinkMarkdown.splitInner("Target Note|friendly label");
    assertThat(s.target(), equalTo("Target Note"));
    assertThat(s.display(), equalTo("friendly label"));
  }

  @Test
  void splitInner_emptyRightSideActsAsNoPipe() {
    WikiLinkMarkdown.WikiInnerSplit s = WikiLinkMarkdown.splitInner("Alpha|");
    assertThat(s.target(), equalTo("Alpha"));
    assertThat(s.display(), equalTo("Alpha"));
  }

  @Test
  void newInnerForKeepVisibleText_qualifiedPlainLink() {
    assertThat(
        WikiLinkMarkdownRewrite.newInnerForKeepVisibleText("MyNb:OldTitle", "NewTitle"),
        equalTo("MyNb:NewTitle|MyNb:OldTitle"));
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
  void newInnerForKeepVisibleText_keepsPathMarkdownLabel() {
    assertThat(
        WikiLinkMarkdownRewrite.newInnerForKeepVisibleText("[label](/Folder/Old.md)", "New"),
        equalTo("[label](/Folder/New.md)"));
  }

  @Test
  void newInnerForKeepVisibleText_emptyPipeUsesTargetAsDisplay() {
    assertThat(
        WikiLinkMarkdownRewrite.newInnerForKeepVisibleText("Alpha|", "Beta"),
        equalTo("Beta|Alpha"));
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
        WikiLinkMarkdownRewrite.replaceWikiLinksMatchingTrimmedInner(
            "see [[  Old  ]] end", "Old", "NewTitle"),
        equalTo("see [[NewTitle]] end"));
  }
}
