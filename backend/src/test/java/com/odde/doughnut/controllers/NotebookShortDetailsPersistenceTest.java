package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class NotebookShortDetailsPersistenceTest extends ControllerTestBase {

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void notebookTableHasShortDetailsColumn() {
    Long count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'notebook'
            AND column_name = 'short_details'
            """,
            Long.class);
    assertTrue(
        count != null && count >= 1L,
        "Run backend/gradlew -p backend migrateTestDB so V300000152 applies (notebook.short_details).");
  }

  @Test
  void backfillFromHeadNoteDetails_matchesMigrationExpression() {
    Note head = makeMe.aNote().title("Head").please();
    Notebook notebook = head.getNotebook();
    head.setDetails("<p>Hello world</p>");
    makeMe.entityPersister.save(head);
    makeMe.entityPersister.flush();

    jdbcTemplate.update(
        """
        UPDATE notebook n
        INNER JOIN notebook_head_note nh ON nh.notebook_id = n.id
        INNER JOIN note hn ON nh.head_note_id = hn.id AND hn.deleted_at IS NULL
        SET n.short_details = CASE
          WHEN hn.details IS NULL THEN NULL
          WHEN TRIM(REGEXP_REPLACE(hn.details, '<[^>]*>', '')) = '' THEN NULL
          WHEN CHAR_LENGTH(TRIM(REGEXP_REPLACE(hn.details, '<[^>]*>', ''))) <= 500
            THEN TRIM(REGEXP_REPLACE(hn.details, '<[^>]*>', ''))
          ELSE LEFT(TRIM(REGEXP_REPLACE(hn.details, '<[^>]*>', '')), 500)
        END
        WHERE n.id = ?
        """,
        notebook.getId());

    String stored =
        jdbcTemplate.queryForObject(
            "SELECT short_details FROM notebook WHERE id = ?", String.class, notebook.getId());
    assertThat(stored, equalTo("Hello world"));
  }
}
