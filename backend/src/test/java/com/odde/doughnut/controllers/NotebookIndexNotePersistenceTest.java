package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class NotebookIndexNotePersistenceTest extends ControllerTestBase {

  @Autowired JdbcTemplate jdbcTemplate;

  @BeforeEach
  void requireNotebookIndexNoteForeignKey() {
    Long count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM information_schema.table_constraints
            WHERE table_schema = DATABASE() AND table_name = 'notebook'
            AND constraint_name = 'fk_notebook_index_note_id'
            """,
            Long.class);
    assertTrue(
        count != null && count >= 1L,
        "Run backend/gradlew -p backend migrateTestDB so V300000191 applies.");
  }

  @Test
  void notebookTableHasIndexNoteIdColumn() {
    Long count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'notebook'
            AND column_name = 'index_note_id'
            """,
            Long.class);
    assertTrue(count != null && count >= 1L);
  }
}
