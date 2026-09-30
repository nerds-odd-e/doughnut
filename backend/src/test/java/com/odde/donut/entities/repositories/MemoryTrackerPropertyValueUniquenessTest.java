package com.odde.donut.entities.repositories;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Note;
import com.odde.donut.testability.SpringTestBase;
import java.util.List;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class MemoryTrackerPropertyValueUniquenessTest extends SpringTestBase {
  @Autowired JdbcTemplate jdbcTemplate;

  Note note;

  @BeforeEach
  void setup() {
    note = makeMe.aNote().please();
  }

  @Test
  void trackersOnDifferentValuesOfTheSameKeyAreBothSaved() {
    makeMe.aMemoryTrackerFor(note).propertyKey("example of").propertyValue("[[run]]").please();
    makeMe
        .aMemoryTrackerFor(note)
        .propertyKey("example of")
        .propertyValue("[[past tense]]")
        .please();
    makeMe.entityPersister.flush();

    List<String> values =
        jdbcTemplate.queryForList(
            "SELECT property_value FROM memory_tracker WHERE note_id = ? AND property_key = ?",
            String.class,
            note.getId(),
            "example of");
    assertThat(values, containsInAnyOrder("[[run]]", "[[past tense]]"));
  }

  @Test
  void aSecondTrackerOnTheSameKeyAndValueIsRefused() {
    makeMe.aMemoryTrackerFor(note).propertyKey("example of").propertyValue("[[run]]").please();
    makeMe.entityPersister.flush();

    assertThrows(
        ConstraintViolationException.class,
        () -> {
          makeMe
              .aMemoryTrackerFor(note)
              .propertyKey("example of")
              .propertyValue("[[run]]")
              .please();
          makeMe.entityPersister.flush();
        });
  }
}
