package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.entities.Note;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class NoteTitlePersistenceTest extends ControllerTestBase {

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void noteTitleColumnIsNotNullable() {
    String isNullable =
        jdbcTemplate.queryForObject(
            """
            SELECT is_nullable FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'note' AND column_name = 'title'
            """,
            String.class);
    assertTrue(
        "NO".equalsIgnoreCase(isNullable),
        "Run backend/gradlew -p backend migrateTestDB so V300000158 applies (note.title NOT NULL).");
  }

  @Test
  void noPersistedNoteHasNullOrBlankTitle() {
    Long count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM note WHERE title IS NULL OR TRIM(title) = ''", Long.class);
    assertTrue(count != null && count == 0L);
  }

  @Test
  void updateNoteTitleToNull_isRejected() {
    Note note = makeMe.aNote().title("HasTitle").please();
    makeMe.entityPersister.flush();

    assertThrows(
        DataIntegrityViolationException.class,
        () -> jdbcTemplate.update("UPDATE note SET title = NULL WHERE id = ?", note.getId()));
  }
}
