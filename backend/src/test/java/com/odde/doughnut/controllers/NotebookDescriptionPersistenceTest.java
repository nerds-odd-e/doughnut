package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class NotebookDescriptionPersistenceTest extends ControllerTestBase {

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void notebookTableHasDescriptionColumn() {
    Long count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'notebook'
            AND column_name = 'description'
            """,
            Long.class);
    assertTrue(
        count != null && count >= 1L,
        "Run backend/gradlew -p backend migrateTestDB so V300000153 applies (notebook.description).");
  }

  @Test
  void backfillFromLegacyRootNoteDetails_matchesMigrationExpression() {
    Note rootNote = makeMe.aNote().title("Head").please();
    Notebook notebook = rootNote.getNotebook();
    rootNote.setDetails("<p>Hello world</p>");
    makeMe.entityPersister.save(rootNote);
    makeMe.entityPersister.flush();

    jdbcTemplate.update(
        """
        UPDATE notebook n
        INNER JOIN note hn ON hn.notebook_id = n.id AND hn.id = ? AND hn.deleted_at IS NULL
        SET n.description = CASE
          WHEN hn.details IS NULL THEN NULL
          WHEN TRIM(REGEXP_REPLACE(hn.details, '<[^>]*>', '')) = '' THEN NULL
          WHEN CHAR_LENGTH(TRIM(REGEXP_REPLACE(hn.details, '<[^>]*>', ''))) <= 500
            THEN TRIM(REGEXP_REPLACE(hn.details, '<[^>]*>', ''))
          ELSE LEFT(TRIM(REGEXP_REPLACE(hn.details, '<[^>]*>', '')), 500)
        END
        WHERE n.id = ?
        """,
        rootNote.getId(),
        notebook.getId());

    String stored =
        jdbcTemplate.queryForObject(
            "SELECT description FROM notebook WHERE id = ?", String.class, notebook.getId());
    assertThat(stored, equalTo("Hello world"));
  }
}
