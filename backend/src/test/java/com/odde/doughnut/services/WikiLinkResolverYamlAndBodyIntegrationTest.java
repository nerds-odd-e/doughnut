package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

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
}
