package com.odde.donut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;

import java.util.List;
import org.junit.jupiter.api.Test;

class AuthoredNoteReferencesTest {

  private static final CanonicalDonutOrigin ORIGIN = CanonicalDonutOrigin.production();

  @Test
  void inOccurrenceOrder_emitsWikiAndRootRelativeNoteIdUrlsInDocumentOrder() {
    List<AuthoredNoteReference> refs =
        AuthoredNoteReferences.inOccurrenceOrder(
            "See [[Folder/Title|wiki]] and [label](/n42) plus [path](/Folder/Title.md).", ORIGIN);

    assertThat(refs.size(), equalTo(2));
    assertThat(refs.getFirst(), instanceOf(AuthoredNoteReference.WikiPortablePathTarget.class));
    AuthoredNoteReference.WikiPortablePathTarget wiki =
        (AuthoredNoteReference.WikiPortablePathTarget) refs.getFirst();
    assertThat(wiki.authoredLink(), equalTo("Folder/Title|wiki"));
    assertThat(wiki.portablePath().format(), equalTo("Folder/Title"));
    assertThat(wiki.displayText(), equalTo("wiki"));

    assertThat(refs.get(1), instanceOf(AuthoredNoteReference.NoteIdUrlTarget.class));
    AuthoredNoteReference.NoteIdUrlTarget url = (AuthoredNoteReference.NoteIdUrlTarget) refs.get(1);
    assertThat(url.authoredLink(), equalTo("[label](/n42)"));
    assertThat(url.noteId(), equalTo(42));
    assertThat(url.href(), equalTo("/n42"));
    assertThat(url.displayText(), equalTo("label"));
  }

  @Test
  void inOccurrenceOrder_emitsAbsoluteCanonicalNoteIdUrls() {
    List<AuthoredNoteReference> refs =
        AuthoredNoteReferences.inOccurrenceOrder(
            "[abs](https://doughnut.odd-e.com/n99) [foreign](https://evil.example/n99)", ORIGIN);

    assertThat(
        refs,
        equalTo(
            List.of(
                new AuthoredNoteReference.NoteIdUrlTarget(
                    "[abs](https://doughnut.odd-e.com/n99)",
                    99,
                    "https://doughnut.odd-e.com/n99",
                    "abs"))));
  }

  @Test
  void inOccurrenceOrder_skipsRetiredRedirectAndPropertyNoteUrls() {
    assertThat(
        AuthoredNoteReferences.inOccurrenceOrder(
            "[a](/n/9) [b](/n9/p/topic) [c](/n9?x=1) [ok](/n9)", ORIGIN),
        equalTo(List.of(new AuthoredNoteReference.NoteIdUrlTarget("[ok](/n9)", 9, "/n9", "ok"))));
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
  void sourceLocalKey_foldsWikiTargetsThatDifferOnlyByCase() {
    AuthoredNoteReference.WikiPortablePathTarget lower =
        AuthoredNoteReference.WikiPortablePathTarget.fromAuthoredInner("target");
    AuthoredNoteReference.WikiPortablePathTarget upper =
        AuthoredNoteReference.WikiPortablePathTarget.fromAuthoredInner("Target");

    assertThat(lower.sourceLocalKey(), equalTo(upper.sourceLocalKey()));
  }

  @Test
  void sourceLocalKey_distinguishesNoteIdUrlTargetsByNoteId() {
    AuthoredNoteReference.NoteIdUrlTarget seven =
        new AuthoredNoteReference.NoteIdUrlTarget("[Target](/n7)", 7, "/n7", "Target");
    AuthoredNoteReference.NoteIdUrlTarget eight =
        new AuthoredNoteReference.NoteIdUrlTarget("[Target](/n8)", 8, "/n8", "Target");

    assertThat(seven.sourceLocalKey(), not(equalTo(eight.sourceLocalKey())));
  }

  @Test
  void uniqueWikiPortablePathTargets_emitsWikiOnlyFromMixedMarkdown() {
    assertThat(
        AuthoredNoteReferences.uniqueWikiPortablePathTargets("[[Alpha]] [Beta](/n9)"),
        equalTo(List.of(AuthoredNoteReference.WikiPortablePathTarget.fromAuthoredInner("Alpha"))));
  }

  private static List<String> wikiAuthoredLinks(String content) {
    return AuthoredNoteReferences.inOccurrenceOrder(content, ORIGIN).stream()
        .filter(AuthoredNoteReference.WikiPortablePathTarget.class::isInstance)
        .map(AuthoredNoteReference.WikiPortablePathTarget.class::cast)
        .map(AuthoredNoteReference.WikiPortablePathTarget::authoredLink)
        .toList();
  }
}
