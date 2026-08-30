package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.algorithms.Frontmatter;
import com.odde.donut.entities.Note;
import com.odde.donut.testability.MakeMe;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WikiLinkResolverPropertyWikiLinkTest {

  @Autowired MakeMe makeMe;
  @Autowired WikiLinkResolver wikiLinkResolver;

  @Test
  void resolveAnyTargetWikiLinkToken_resolvesWhenExactDecodedPropertyExists() {
    Note moon = moonWithExactProperty("a part of");

    var resolved =
        wikiLinkResolver.resolveAnyTargetWikiLinkToken("Moon#prop:a%20part%20of", focusOn(moon));

    assertThat(resolved.orElseThrow().getId(), equalTo(moon.getId()));
  }

  @Test
  void resolveAnyTargetWikiLinkToken_acceptsLowercaseHexInEncodedPropertyKey() {
    Note moon = moonWithExactProperty("a/b");

    var resolved = wikiLinkResolver.resolveAnyTargetWikiLinkToken("Moon#prop:a%2fb", focusOn(moon));

    assertThat(resolved.orElseThrow().getId(), equalTo(moon.getId()));
  }

  @Test
  void resolveAnyTargetWikiLinkToken_emptyWhenExactPropertyAbsent() {
    Note moon = makeMe.aNote().title("Moon").notebookOwnedBy(makeMe.aUser().please()).please();

    assertThat(
        wikiLinkResolver.resolveAnyTargetWikiLinkToken("Moon#prop:a%20part%20of", focusOn(moon)),
        equalTo(Optional.empty()));
  }

  @Test
  void resolveAnyTargetWikiLinkToken_emptyWhenDecodedKeyCaseDiffers() {
    Note moon = moonWithExactProperty("WikiData");

    assertThat(
        wikiLinkResolver.resolveAnyTargetWikiLinkToken("Moon#prop:wikidata", focusOn(moon)),
        equalTo(Optional.empty()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Moon#prop:%ZZ", "Moon#prop:", "Moon#prop:%80"})
  void resolveAnyTargetWikiLinkToken_emptyWhenEncodedPropertyKeyInvalid(String token) {
    Note moon = moonWithExactProperty("a part of");

    assertThat(
        wikiLinkResolver.resolveAnyTargetWikiLinkToken(token, focusOn(moon)),
        equalTo(Optional.empty()));
  }

  @Test
  void resolveAnyTargetWikiLinkToken_matchesNoteOnlyToken() {
    Note moon = makeMe.aNote().title("Moon").notebookOwnedBy(makeMe.aUser().please()).please();

    var resolved = wikiLinkResolver.resolveAnyTargetWikiLinkToken("Moon", focusOn(moon));

    assertThat(resolved.orElseThrow().getId(), equalTo(moon.getId()));
  }

  private Note moonWithExactProperty(String yamlKey) {
    return makeMe
        .aNote()
        .title("Moon")
        .notebookOwnedBy(makeMe.aUser().please())
        .content(Frontmatter.empty().set(yamlKey, "v").fenced(""))
        .please();
  }

  private Note focusOn(Note moon) {
    return makeMe.aNote().underSameNotebookAs(moon).please();
  }
}
