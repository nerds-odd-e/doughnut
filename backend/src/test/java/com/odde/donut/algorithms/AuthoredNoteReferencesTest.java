package com.odde.donut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import java.util.List;
import org.junit.jupiter.api.Test;

class AuthoredNoteReferencesTest {

  @Test
  void inOccurrenceOrder_emitsOnlyWikiPortablePathTargets() {
    List<AuthoredNoteReference> refs =
        AuthoredNoteReferences.inOccurrenceOrder(
            "See [[Folder/Title|wiki]] and [label](/n42) plus [path](/Folder/Title.md).");

    assertThat(refs.size(), equalTo(1));
    assertThat(refs.getFirst(), instanceOf(AuthoredNoteReference.WikiPortablePathTarget.class));
    AuthoredNoteReference.WikiPortablePathTarget wiki =
        (AuthoredNoteReference.WikiPortablePathTarget) refs.getFirst();
    assertThat(wiki.authoredLink(), equalTo("Folder/Title|wiki"));
    assertThat(wiki.portablePath().format(), equalTo("Folder/Title"));
    assertThat(wiki.displayText(), equalTo("wiki"));
  }

  @Test
  void inOccurrenceOrder_readsWikiLinkFromParsedFrontmatterScalar() {
    String title = "In volitional (\"let's\" or \"I shall\") statements";
    String content = Frontmatter.empty().set("example of", "[[" + title + "]]").fenced("");

    assertThat(wikiAuthoredLinks(content), equalTo(List.of(title)));
  }

  @Test
  void inOccurrenceOrder_readsWikiLinksFromListItemsInYamlOrder() {
    String content =
        Frontmatter.parse(
                """
                example of:
                  - "[[First]]"
                  - plain
                  - "[[Second]]"
                """)
            .fenced("");

    assertThat(wikiAuthoredLinks(content), equalTo(List.of("First", "Second")));
  }

  @Test
  void inOccurrenceOrder_scansScalarsThenListItemsInPropertyOrder() {
    String content =
        Frontmatter.parse(
                """
                scalar: "[[Scalar]]"
                listed:
                  - "[[One]]"
                  - "[[Two]]"
                """)
            .fenced("Body [[Body]]");

    assertThat(wikiAuthoredLinks(content), equalTo(List.of("Scalar", "One", "Two", "Body")));
  }

  @Test
  void inOccurrenceOrder_skipsUnsupportedNestedValues() {
    String content =
        Frontmatter.parse(
                """
                nested:
                  child: "[[Nested]]"
                listed:
                  - "[[Listed]]"
                """)
            .fenced("");

    assertThat(wikiAuthoredLinks(content), equalTo(List.of("Listed")));
  }

  @Test
  void uniquePreserveOrder_keepsWikiAndNoteIdUrlKindsDistinct() {
    AuthoredNoteReference.WikiPortablePathTarget wiki =
        AuthoredNoteReference.WikiPortablePathTarget.fromAuthoredInner("Target");
    AuthoredNoteReference.NoteIdUrlTarget url =
        new AuthoredNoteReference.NoteIdUrlTarget("[Target](/n7)", 7, "/n7", "Target");

    assertThat(
        AuthoredNoteReferences.uniquePreserveOrder(List.of(wiki, url, wiki)),
        equalTo(List.of(wiki, url)));
  }

  @Test
  void uniqueWikiPortablePathTargets_emitsWikiOnlyFromMixedMarkdown() {
    assertThat(
        AuthoredNoteReferences.uniqueWikiPortablePathTargets("[[Alpha]] [Beta](/n9)"),
        equalTo(List.of(AuthoredNoteReference.WikiPortablePathTarget.fromAuthoredInner("Alpha"))));
  }

  private static List<String> wikiAuthoredLinks(String content) {
    return AuthoredNoteReferences.inOccurrenceOrder(content).stream()
        .filter(AuthoredNoteReference.WikiPortablePathTarget.class::isInstance)
        .map(AuthoredNoteReference.WikiPortablePathTarget.class::cast)
        .map(AuthoredNoteReference.WikiPortablePathTarget::authoredLink)
        .toList();
  }
}
