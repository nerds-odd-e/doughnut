package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RelationType;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import java.util.Optional;
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
    Note parent = makeMe.aNote().title("Alpha").creatorAndOwner(owner).please();
    Note child = makeMe.aNote().title("Child").under(parent).please();
    child.setDetails("---\nparent: \"[[Alpha]]\"\n---\n\nBody line.");
    makeMe.entityPersister.merge(child);
    makeMe.entityPersister.flush();

    assertThat(wikiLinkResolver.resolveWikiLinksForCache(child, owner).size(), equalTo(1));
  }

  @Test
  void wikiLinkResolver_findsPlainWikiLinkInBody() {
    User owner = makeMe.aUser().please();
    Note parent = makeMe.aNote().title("Alpha").creatorAndOwner(owner).please();
    Note child = makeMe.aNote().title("Child").under(parent).details("See [[Alpha]]").please();
    makeMe.entityPersister.flush();
    makeMe.entityPersister.refresh(parent);

    assertThat(parent.getChildren().size(), equalTo(1));
    assertThat(wikiLinkResolver.resolveWikiLinksForCache(child, owner).size(), equalTo(1));
  }

  @Test
  void resolvesRelationshipTargetFromYaml() {
    User owner = makeMe.aUser().please();
    Note source = makeMe.aNote("Src").creatorAndOwner(owner).please();
    Note target = makeMe.aNote("Tgt").under(source).please();
    Note relation =
        makeMe.aRelation().between(source, target, RelationType.PART).under(source).please();

    assertThat(
        wikiLinkResolver.resolveSemanticTarget(relation, owner).map(Note::getId),
        equalTo(Optional.of(target.getId())));
  }

  @Test
  void resolveWikiInnerTitle_matchesNotebookScopedTitle() {
    User owner = makeMe.aUser().please();
    Note parent = makeMe.aNote().title("Alpha").creatorAndOwner(owner).please();
    Note child = makeMe.aNote().title("Child").under(parent).please();
    assertThat(
        wikiLinkResolver.resolveWikiInnerTitle("Alpha", owner, child).map(Note::getId),
        equalTo(Optional.of(parent.getId())));
  }
}
