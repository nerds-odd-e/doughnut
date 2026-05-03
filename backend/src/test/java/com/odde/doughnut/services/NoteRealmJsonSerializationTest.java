package com.odde.doughnut.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RelationType;
import com.odde.doughnut.entities.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteRealmJsonSerializationTest {

  @Autowired com.odde.doughnut.testability.MakeMe makeMe;
  @Autowired NoteRealmService noteRealmService;
  @Autowired WikiTitleCacheService wikiTitleCacheService;
  @Autowired EntityManager entityManager;

  @Test
  void serializes_realm_when_focus_note_is_uninitialized_proxy() throws Exception {
    User user = makeMe.aUser().please();
    Note real = makeMe.aNote().creatorAndOwner(user).title("Head").please();
    entityManager.flush();
    entityManager.clear();

    Note proxyFocus = entityManager.getReference(Note.class, real.getId());
    NoteRealm realm = noteRealmService.build(proxyFocus, user);

    ObjectMapper mapper = new ObjectMapperConfig().objectMapper();
    mapper.writeValueAsString(realm);
  }

  @Test
  void serializes_realm_with_wiki_cache_references() throws Exception {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Note focal = makeMe.aNote().title("Focal").please();
    Note subject = makeMe.aNote().please();
    Note relation = makeMe.aRelation().between(subject, focal).please();
    relation.setDetails(
        RelationshipNoteMarkdownFormatter.formatForRelationshipNote(
            relation, RelationType.SPECIALIZE, subject, focal, null));
    makeMe.entityPersister.merge(relation);
    makeMe.entityPersister.flush();
    wikiTitleCacheService.refreshForNote(relation, user);

    NoteRealm realm = noteRealmService.build(subject, user);

    ObjectMapper mapper = new ObjectMapperConfig().objectMapper();
    mapper.writeValueAsString(realm);
  }
}
