package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.testability.MakeMe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WikiLinkResolverYamlAndBodyIntegrationTest {

  @Autowired MakeMe makeMe;
  @Autowired WikiLinkResolver wikiLinkResolver;

  @Test
  void wikiLinkResolver_findsParentLinkInsideYamlFrontmatter() {
    User owner = makeMe.aUser().please();
    Note parent = makeMe.aNote().title("Alpha").notebookOwnedBy(owner).please();
    Note child =
        makeMe
            .aNote()
            .title("Child")
            .underSameNotebookAs(parent)
            .content("---\nparent: \"[[Alpha]]\"\n---\n\nBody line.")
            .please();

    assertThat(wikiLinkResolver.resolveWikiLinksForCache(child, owner).size(), equalTo(1));
  }

  @Test
  void wikiLinkResolver_findsPlainWikiLinkInBody() {
    User owner = makeMe.aUser().please();
    Note parent = makeMe.aNote().title("Alpha").notebookOwnedBy(owner).please();
    Note child =
        makeMe.aNote().title("Child").underSameNotebookAs(parent).content("See [[Alpha]]").please();

    assertThat(wikiLinkResolver.resolveWikiLinksForCache(child, owner).size(), equalTo(1));
  }

  @Test
  void wikiLinkResolver_resolvesTargetBeforePipe() {
    User owner = makeMe.aUser().please();
    Note parent = makeMe.aNote().title("Alpha").notebookOwnedBy(owner).please();
    Note child =
        makeMe
            .aNote()
            .title("Child")
            .underSameNotebookAs(parent)
            .content("See [[Alpha|friendly alias]]")
            .please();

    var resolved = wikiLinkResolver.resolveWikiLinksForCache(child, owner);
    assertThat(resolved.size(), equalTo(1));
    assertThat(resolved.getFirst().linkText(), equalTo("Alpha|friendly alias"));
    assertThat(resolved.getFirst().targetNote().getId(), equalTo(parent.getId()));
  }

  @Test
  void wikiLinkResolver_resolvesUnambiguousFrontmatterAliasInFocusNotebook() {
    User owner = makeMe.aUser().please();
    Note aliasTarget =
        makeMe.aNote().title("colour").notebookOwnedBy(owner).aliases("color").please();
    Note linker = makeMe.aNote().underSameNotebookAs(aliasTarget).content("See [[color]]").please();

    var resolved = wikiLinkResolver.resolveWikiLinksForCache(linker, owner);
    assertThat(resolved.size(), equalTo(1));
    assertThat(resolved.getFirst().targetNote().getId(), equalTo(aliasTarget.getId()));
  }

  @Test
  void wikiLinkResolver_resolvesQualifiedNotebookAliasLink() {
    User owner = makeMe.aUser().please();
    Notebook otherNotebook =
        makeMe.aNotebook().creatorAndOwner(owner).name("Other Notebook").please();
    Note aliasTarget =
        makeMe
            .aNote()
            .title("Canonical Title")
            .notebook(otherNotebook)
            .aliases("LinkedAlias")
            .please();
    Notebook mainNotebook = makeMe.aNotebook().creatorAndOwner(owner).name("Main").please();
    Note linker =
        makeMe
            .aNote()
            .notebook(mainNotebook)
            .content("See [[Other Notebook:LinkedAlias]]")
            .please();

    var resolved = wikiLinkResolver.resolveWikiLinksForCache(linker, owner);
    assertThat(resolved.size(), equalTo(1));
    assertThat(resolved.getFirst().linkText(), equalTo("Other Notebook:LinkedAlias"));
    assertThat(resolved.getFirst().targetNote().getId(), equalTo(aliasTarget.getId()));
  }

  @Test
  void wikiLinkResolver_exactTitleWinsOverFrontmatterAlias() {
    User owner = makeMe.aUser().please();
    Note byTitle = makeMe.aNote().title("color").notebookOwnedBy(owner).please();
    makeMe.aNote().title("colour").underSameNotebookAs(byTitle).aliases("color").please();
    Note linker = makeMe.aNote().underSameNotebookAs(byTitle).content("See [[color]]").please();

    var resolved = wikiLinkResolver.resolveWikiLinksForCache(linker, owner);
    assertThat(resolved.size(), equalTo(1));
    assertThat(resolved.getFirst().targetNote().getId(), equalTo(byTitle.getId()));
  }

  @Test
  void wikiLinkResolver_resolvesAmbiguousAliasToLowestNoteId() {
    User owner = makeMe.aUser().please();
    Note firstTarget =
        makeMe.aNote().title("first").notebookOwnedBy(owner).aliases("color").please();
    Note secondTarget =
        makeMe.aNote().title("second").underSameNotebookAs(firstTarget).aliases("color").please();
    Note linker = makeMe.aNote().underSameNotebookAs(firstTarget).content("See [[color]]").please();

    var resolved = wikiLinkResolver.resolveWikiLinksForCache(linker, owner);
    assertThat(resolved.size(), equalTo(1));
    assertThat(resolved.getFirst().targetNote().getId(), equalTo(firstTarget.getId()));
    assertThat(firstTarget.getId(), lessThan(secondTarget.getId()));
  }

  @Test
  void wikiLinkResolver_skipsUnreadableLowestIdAliasCandidateForReadableTarget() {
    User secretOwner = makeMe.aUser().please();
    User viewer = makeMe.aUser().please();
    String sharedNotebookName = "Shared Notebook";
    Notebook secretNotebook =
        makeMe.aNotebook().creatorAndOwner(secretOwner).name(sharedNotebookName).please();
    Note unreadableTarget =
        makeMe.aNote().title("hidden").notebook(secretNotebook).aliases("term").please();

    Notebook readableNotebook =
        makeMe.aNotebook().creatorAndOwner(viewer).name(sharedNotebookName).please();
    makeMe.aBazaarNotebook(readableNotebook).please();
    Note readableTarget =
        makeMe.aNote().title("visible").notebook(readableNotebook).aliases("term").please();
    assertThat(unreadableTarget.getId(), lessThan(readableTarget.getId()));

    Notebook viewerNotebook = makeMe.aNotebook().creatorAndOwner(viewer).name("Main").please();
    Note linker =
        makeMe
            .aNote()
            .notebook(viewerNotebook)
            .content("See [[" + sharedNotebookName + ":term]]")
            .please();

    var resolved = wikiLinkResolver.resolveWikiLinksForCache(linker, viewer);
    assertThat(resolved.size(), equalTo(1));
    assertThat(resolved.getFirst().targetNote().getId(), equalTo(readableTarget.getId()));
  }

  @Test
  void does_not_resolve_alias_target_from_wiki_link_item_under_aliases() {
    User owner = makeMe.aUser().please();
    Note overlapDeclarer =
        makeMe
            .aNote()
            .title("Overlap Declarer")
            .notebookOwnedBy(owner)
            .wikiLinkUnderAliases("Other Note")
            .please();
    Note linkerByInnerTitle =
        makeMe.aNote().underSameNotebookAs(overlapDeclarer).content("See [[Other Note]]").please();

    assertThat(wikiLinkResolver.resolveWikiLinksForCache(linkerByInnerTitle, owner), empty());
  }

  @Test
  void resolves_plain_alias_and_ignores_wiki_link_item_under_aliases_in_mixed_list() {
    User owner = makeMe.aUser().please();
    Note mixedAliasNote =
        makeMe
            .aNote()
            .title("colour")
            .notebookOwnedBy(owner)
            .aliases("color")
            .wikiLinkUnderAliases("Other Note")
            .please();
    Note plainAliasLinker =
        makeMe.aNote().underSameNotebookAs(mixedAliasNote).content("See [[color]]").please();
    Note overlapTitleLinker =
        makeMe.aNote().underSameNotebookAs(mixedAliasNote).content("See [[Other Note]]").please();

    var plainResolved = wikiLinkResolver.resolveWikiLinksForCache(plainAliasLinker, owner);
    assertThat(plainResolved.size(), equalTo(1));
    assertThat(plainResolved.getFirst().targetNote().getId(), equalTo(mixedAliasNote.getId()));

    assertThat(wikiLinkResolver.resolveWikiLinksForCache(overlapTitleLinker, owner), empty());
  }

  @Test
  void resolveAnyTargetWikiLinkToken_matchesNoteTargetPortionOfPropertyToken() {
    User owner = makeMe.aUser().please();
    Note moon = makeMe.aNote().title("Moon").notebookOwnedBy(owner).please();
    Note focus = makeMe.aNote().underSameNotebookAs(moon).please();

    var resolved = wikiLinkResolver.resolveAnyTargetWikiLinkToken("Moon#prop:a%20part%20of", focus);

    assertThat(resolved.orElseThrow().getId(), equalTo(moon.getId()));
  }

  @Test
  void resolveAnyTargetWikiLinkToken_matchesNoteOnlyToken() {
    User owner = makeMe.aUser().please();
    Note moon = makeMe.aNote().title("Moon").notebookOwnedBy(owner).please();
    Note focus = makeMe.aNote().underSameNotebookAs(moon).please();

    var resolved = wikiLinkResolver.resolveAnyTargetWikiLinkToken("Moon", focus);

    assertThat(resolved.orElseThrow().getId(), equalTo(moon.getId()));
  }
}
