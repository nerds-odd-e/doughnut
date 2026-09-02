package com.odde.donut.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.configs.ObjectMapperConfig;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
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

  @Autowired com.odde.donut.testability.MakeMe makeMe;
  @Autowired NoteRealmService noteRealmService;
  @Autowired EntityManager entityManager;

  @Test
  void serializes_realm_when_focus_note_is_uninitialized_proxy() throws Exception {
    User user = makeMe.aUser().please();
    Note real = makeMe.aNote().notebookOwnedBy(user).title("Head").please();
    entityManager.flush();
    entityManager.clear();

    Note proxyFocus = entityManager.getReference(Note.class, real.getId());
    NoteRealm realm = noteRealmService.build(proxyFocus, user);

    ObjectMapper mapper = new ObjectMapperConfig().objectMapper();
    mapper.writeValueAsString(realm);
  }

  @Test
  void serializes_realm_with_live_note_references() throws Exception {
    User user = makeMe.aUser().please();
    Note focal = makeMe.aNote().title("Focal").notebookOwnedBy(user).please();
    Note subject = makeMe.aNote().title("Subject").underSameNotebookAs(focal).please();
    Note relation =
        makeMe
            .aNote()
            .underSameNotebookAs(focal)
            .withWikiLinksInFrontmatter(subject, focal)
            .please();

    NoteRealm realm = noteRealmService.build(subject, user);

    ObjectMapper mapper = new ObjectMapperConfig().objectMapper();
    mapper.writeValueAsString(realm);
  }
}
