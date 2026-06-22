package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

class WikiLinkMarkdownTest {

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
  void newInnerForUpdateVisibleText_plainLink() {
    assertThat(
        WikiLinkMarkdown.newInnerForUpdateVisibleText("OldTitle", "NewTitle"), equalTo("NewTitle"));
  }

  @Test
  void newInnerForUpdateVisibleText_keepsDisplaySegment() {
    assertThat(
        WikiLinkMarkdown.newInnerForUpdateVisibleText("OldTitle|custom label", "NewTitle"),
        equalTo("NewTitle|custom label"));
  }

  @Test
  void newInnerForUpdateVisibleText_keepsNotebookQualifier() {
    assertThat(
        WikiLinkMarkdown.newInnerForUpdateVisibleText("MyNb:OldTitle|x", "NewTitle"),
        equalTo("MyNb:NewTitle|x"));
  }

  @Test
  void newInnerForKeepVisibleText_plainLinkAddsDisplay() {
    assertThat(
        WikiLinkMarkdown.newInnerForKeepVisibleText("OldTitle", "NewTitle"),
        equalTo("NewTitle|OldTitle"));
  }

  @Test
  void newInnerForKeepVisibleText_preservesCustomDisplay() {
    assertThat(
        WikiLinkMarkdown.newInnerForKeepVisibleText("OldTitle|custom text", "NewTitle"),
        equalTo("NewTitle|custom text"));
  }

  @Test
  void newInnerForKeepVisibleText_qualifiedPlainLink() {
    assertThat(
        WikiLinkMarkdown.newInnerForKeepVisibleText("MyNb:OldTitle", "NewTitle"),
        equalTo("MyNb:NewTitle|MyNb:OldTitle"));
  }

  @Test
  void newInnerForKeepVisibleText_emptyPipeUsesTargetAsDisplay() {
    assertThat(
        WikiLinkMarkdown.newInnerForKeepVisibleText("Alpha|", "Beta"), equalTo("Beta|Alpha"));
  }

  @Test
  void newInnerForQualifyUnqualifiedOutgoingLink_plainLinkAddsSourceNotebookAndDisplay() {
    assertThat(
        WikiLinkMarkdown.newInnerForQualifyUnqualifiedOutgoingLink("Target", "Source Notebook"),
        equalTo("Source Notebook:Target|Target"));
  }

  @Test
  void newInnerForQualifyUnqualifiedOutgoingLink_preservesCustomDisplay() {
    assertThat(
        WikiLinkMarkdown.newInnerForQualifyUnqualifiedOutgoingLink(
            "Target|friendly label", "Source Notebook"),
        equalTo("Source Notebook:Target|friendly label"));
  }

  @Test
  void newInnerForQualifyUnqualifiedOutgoingLink_emptyPipeUsesTargetAsDisplay() {
    assertThat(
        WikiLinkMarkdown.newInnerForQualifyUnqualifiedOutgoingLink("Target|", "Source Notebook"),
        equalTo("Source Notebook:Target|Target"));
  }

  @Test
  void newInnerForQualifyUnqualifiedOutgoingLink_keepsAlreadyQualifiedPlainLink() {
    assertThat(
        WikiLinkMarkdown.newInnerForQualifyUnqualifiedOutgoingLink(
            "Other Notebook:Target", "Source Notebook"),
        equalTo("Other Notebook:Target"));
  }

  @Test
  void newInnerForQualifyUnqualifiedOutgoingLink_keepsAlreadyQualifiedDisplayLink() {
    assertThat(
        WikiLinkMarkdown.newInnerForQualifyUnqualifiedOutgoingLink(
            "Other Notebook:Target|friendly label", "Source Notebook"),
        equalTo("Other Notebook:Target|friendly label"));
  }

  @Test
  void newInnerForQualifyUnqualifiedOutgoingLink_keepsBlankInner() {
    assertThat(
        WikiLinkMarkdown.newInnerForQualifyUnqualifiedOutgoingLink("   ", "Source Notebook"),
        equalTo("   "));
  }

  @Test
  void replaceWikiLinksMatchingTrimmedInner_matchesWhitespaceInsideBrackets() {
    assertThat(
        WikiLinkMarkdown.replaceWikiLinksMatchingTrimmedInner(
            "see [[  Old  ]] end", "Old", "NewTitle"),
        equalTo("see [[NewTitle]] end"));
  }
}
