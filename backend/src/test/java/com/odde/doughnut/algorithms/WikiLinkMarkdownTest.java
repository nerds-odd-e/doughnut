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
  void replaceWikiLinksMatchingTrimmedInner_matchesWhitespaceInsideBrackets() {
    assertThat(
        WikiLinkMarkdown.replaceWikiLinksMatchingTrimmedInner(
            "see [[  Old  ]] end", "Old", "NewTitle"),
        equalTo("see [[NewTitle]] end"));
  }
}
