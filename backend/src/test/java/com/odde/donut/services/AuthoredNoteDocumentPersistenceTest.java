package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.testability.MakeMe;
import com.odde.donut.validators.AuthoredNoteContent;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthoredNoteDocumentPersistenceTest {

  @Autowired MakeMe makeMe;
  @Autowired AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence;
  @Autowired NoteRepository noteRepository;
  @Autowired CanonicalDonutOrigin canonicalDonutOrigin;
  @Autowired EntityManager entityManager;

  /**
   * A Spring MVC {@code @PathVariable} note converter loads the entity outside this method's
   * transaction, so the note handed to a controller is detached by the time the controller's own
   * {@code @Transactional} body runs. Flushing then clearing the whole persistence context (rather
   * than detaching just this one entity) reproduces that same condition without leaving Hibernate's
   * per-entity collection bookkeeping in the inconsistent state a single {@code detach()} call
   * does.
   */
  private Note detachedExistingNote() {
    User user = makeMe.aUser().please();
    Note note = makeMe.aNote().title("Sample").notebookOwnedBy(user).please();
    entityManager.flush();
    entityManager.clear();
    return note;
  }

  @Test
  void persistsContentForADetachedExistingNoteWithoutThrowing() {
    Note note = detachedExistingNote();

    AuthoredNoteDocument document =
        AuthoredNoteContent.prepareDocumentForSave("Updated body.", canonicalDonutOrigin);

    authoredNoteDocumentPersistence.persist(note, document, new Timestamp(0));

    Note persisted = noteRepository.findById(note.getId()).orElseThrow();
    assertThat(persisted.getContent(), equalTo("---\ntype: Note\n---\nUpdated body."));
  }
}
