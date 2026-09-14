package com.odde.donut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.donut.entities.Note;
import java.util.List;
import java.util.Map;
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
        "Run backend/gradlew -p backend migrateTestDB so note.title is NOT NULL.");
  }

  @Test
  void noteTitleUniquenessIndexUsesStoredGeneratedColumns() {
    List<Map<String, Object>> columns =
        jdbcTemplate.queryForList(
            """
            SELECT column_name, extra, generation_expression
            FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'note'
              AND column_name IN (
                'title_uniqueness_notebook_id',
                'title_uniqueness_folder_id',
                'title_uniqueness_key'
              )
            ORDER BY ordinal_position
            """);
    assertEquals(
        List.of(
            List.of("title_uniqueness_notebook_id", "STORED GENERATED", "ifnull(`notebook_id`,0)"),
            List.of("title_uniqueness_folder_id", "STORED GENERATED", "ifnull(`folder_id`,0)"),
            List.of("title_uniqueness_key", "STORED GENERATED", "lower(`title`)")),
        columns.stream()
            .map(
                column ->
                    List.of(
                        column.get("column_name"),
                        column.get("extra"),
                        column.get("generation_expression")))
            .toList());

    List<Map<String, Object>> indexParts =
        jdbcTemplate.queryForList(
            """
            SELECT column_name, expression
            FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'note'
              AND index_name = 'uk_note_notebook_folder_title'
            ORDER BY seq_in_index
            """);
    assertEquals(
        List.of(
            "title_uniqueness_notebook_id", "title_uniqueness_folder_id", "title_uniqueness_key"),
        indexParts.stream().map(part -> part.get("column_name")).toList());
    assertTrue(indexParts.stream().allMatch(part -> part.get("expression") == null));

    assertEquals(
        "RESTRICT",
        jdbcTemplate.queryForObject(
            """
            SELECT delete_rule
            FROM information_schema.referential_constraints
            WHERE constraint_schema = DATABASE() AND table_name = 'note'
              AND constraint_name = 'fk_note_folder'
            """,
            String.class));
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
