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
class RelationshipNoteEndpointResolverTest {

  @Autowired RelationshipNoteEndpointResolver resolver;
  @Autowired MakeMe makeMe;

  @Test
  void resolvesTargetFromRelationshipYaml() {
    User owner = makeMe.aUser().please();
    Note source = makeMe.aNote("Src").creatorAndOwner(owner).please();
    Note target = makeMe.aNote("Tgt").under(source).please();
    Note relation =
        makeMe.aRelation().between(source, target, RelationType.PART).under(source).please();
    relation.setTargetNote(null);
    makeMe.entityPersister.merge(relation);
    makeMe.entityPersister.flush();

    assertThat(
        resolver.resolveSemanticTarget(relation, owner).map(Note::getId),
        equalTo(Optional.of(target.getId())));
  }
}
