package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
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
  @Autowired NoteAliasIndexService noteAliasIndexService;

  @Test
  void wikiLinkResolver_findsParentLinkInsideYamlFrontmatter() {
    User owner = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
    Note parent = makeMe.aNote().title("Alpha").notebook(notebook).please();
    Note child = makeMe.aNote().title("Child").notebook(notebook).please();
    child.setContent("---\nparent: \"[[Alpha]]\"\n---\n\nBody line.");
    makeMe.entityPersister.merge(child);
    makeMe.entityPersister.flush();

    assertThat(wikiLinkResolver.resolveWikiLinksForCache(child, owner).size(), equalTo(1));
  }

  @Test
  void wikiLinkResolver_findsPlainWikiLinkInBody() {
    User owner = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
    Note parent = makeMe.aNote().title("Alpha").notebook(notebook).please();
    Note child = makeMe.aNote().title("Child").notebook(notebook).content("See [[Alpha]]").please();
    makeMe.entityPersister.flush();
    makeMe.entityPersister.refresh(parent);

    assertThat(wikiLinkResolver.resolveWikiLinksForCache(child, owner).size(), equalTo(1));
  }

  @Test
  void wikiLinkResolver_resolvesTargetBeforePipe() {
    User owner = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
    Note parent = makeMe.aNote().title("Alpha").notebook(notebook).please();
    Note child =
        makeMe
            .aNote()
            .title("Child")
            .notebook(notebook)
            .content("See [[Alpha|friendly alias]]")
            .please();
    makeMe.entityPersister.flush();
    makeMe.entityPersister.refresh(parent);

    var resolved = wikiLinkResolver.resolveWikiLinksForCache(child, owner);
    assertThat(resolved.size(), equalTo(1));
    assertThat(resolved.getFirst().linkText(), equalTo("Alpha|friendly alias"));
    assertThat(resolved.getFirst().targetNote().getId(), equalTo(parent.getId()));
  }

  @Test
  void wikiLinkResolver_resolvesUnambiguousFrontmatterAliasInFocusNotebook() {
    User owner = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
    String aliasTargetMarkdown = "---\naliases:\n  - color\n---\n\nbody";
    Note aliasTarget =
        makeMe.aNote().title("colour").notebook(notebook).content(aliasTargetMarkdown).please();
    noteAliasIndexService.refreshForNote(aliasTarget);
    Note linker = makeMe.aNote().notebook(notebook).content("See [[color]]").please();
    makeMe.entityPersister.flush();

    var resolved = wikiLinkResolver.resolveWikiLinksForCache(linker, owner);
    assertThat(resolved.size(), equalTo(1));
    assertThat(resolved.getFirst().linkText(), equalTo("color"));
    assertThat(resolved.getFirst().targetNote().getId(), equalTo(aliasTarget.getId()));
  }

  @Test
  void wikiLinkResolver_resolvesQualifiedNotebookAliasLink() {
    User owner = makeMe.aUser().please();
    Notebook otherNotebook =
        makeMe.aNotebook().creatorAndOwner(owner).name("Other Notebook").please();
    String aliasTargetMarkdown = "---\naliases:\n  - LinkedAlias\n---\n\nbody";
    Note aliasTarget =
        makeMe
            .aNote()
            .title("Canonical Title")
            .notebook(otherNotebook)
            .content(aliasTargetMarkdown)
            .please();
    noteAliasIndexService.refreshForNote(aliasTarget);
    Notebook mainNotebook = makeMe.aNotebook().creatorAndOwner(owner).name("Main").please();
    Note linker =
        makeMe
            .aNote()
            .notebook(mainNotebook)
            .content("See [[Other Notebook:LinkedAlias]]")
            .please();
    makeMe.entityPersister.flush();

    var resolved = wikiLinkResolver.resolveWikiLinksForCache(linker, owner);
    assertThat(resolved.size(), equalTo(1));
    assertThat(resolved.getFirst().linkText(), equalTo("Other Notebook:LinkedAlias"));
    assertThat(resolved.getFirst().targetNote().getId(), equalTo(aliasTarget.getId()));
  }

  @Test
  void wikiLinkResolver_exactTitleWinsOverFrontmatterAlias() {
    User owner = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
    Note byTitle = makeMe.aNote().title("color").notebook(notebook).please();
    String aliasTargetMarkdown = "---\naliases:\n  - color\n---\n\nbody";
    Note byAlias =
        makeMe.aNote().title("colour").notebook(notebook).content(aliasTargetMarkdown).please();
    noteAliasIndexService.refreshForNote(byAlias);
    Note linker = makeMe.aNote().notebook(notebook).content("See [[color]]").please();
    makeMe.entityPersister.flush();

    var resolved = wikiLinkResolver.resolveWikiLinksForCache(linker, owner);
    assertThat(resolved.size(), equalTo(1));
    assertThat(resolved.getFirst().targetNote().getId(), equalTo(byTitle.getId()));
  }

  @Test
  void wikiLinkResolver_resolvesAmbiguousAliasToLowestNoteId() {
    User owner = makeMe.aUser().please();
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(owner).please();
    String aliasTargetMarkdown = "---\naliases:\n  - color\n---\n\nbody";
    Note firstTarget =
        makeMe.aNote().title("first").notebook(notebook).content(aliasTargetMarkdown).please();
    Note secondTarget =
        makeMe.aNote().title("second").notebook(notebook).content(aliasTargetMarkdown).please();
    noteAliasIndexService.refreshForNote(firstTarget);
    noteAliasIndexService.refreshForNote(secondTarget);
    Note linker = makeMe.aNote().notebook(notebook).content("See [[color]]").please();
    makeMe.entityPersister.flush();

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
    String aliasTargetMarkdown = "---\naliases:\n  - term\n---\n\nbody";
    Note unreadableTarget =
        makeMe
            .aNote()
            .title("hidden")
            .notebook(secretNotebook)
            .content(aliasTargetMarkdown)
            .please();
    noteAliasIndexService.refreshForNote(unreadableTarget);

    Notebook readableNotebook =
        makeMe.aNotebook().creatorAndOwner(viewer).name(sharedNotebookName).please();
    makeMe.aBazaarNotebook(readableNotebook).please();
    Note readableTarget =
        makeMe
            .aNote()
            .title("visible")
            .notebook(readableNotebook)
            .content(aliasTargetMarkdown)
            .please();
    noteAliasIndexService.refreshForNote(readableTarget);
    assertThat(unreadableTarget.getId(), lessThan(readableTarget.getId()));

    Notebook viewerNotebook = makeMe.aNotebook().creatorAndOwner(viewer).name("Main").please();
    Note linker =
        makeMe
            .aNote()
            .notebook(viewerNotebook)
            .content("See [[" + sharedNotebookName + ":term]]")
            .please();
    makeMe.entityPersister.flush();

    var resolved = wikiLinkResolver.resolveWikiLinksForCache(linker, viewer);
    assertThat(resolved.size(), equalTo(1));
    assertThat(resolved.getFirst().targetNote().getId(), equalTo(readableTarget.getId()));
  }
}
