package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class WikiNotebookSlugPersistenceTest extends ControllerTestBase {

  @Autowired JdbcTemplate jdbcTemplate;

  @BeforeEach
  void requireUniqueSlugIndexes() {
    Long folderUk =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'folder'
            AND index_name = 'uk_folder_notebook_slug'
            """,
            Long.class);
    Long noteUk =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'note'
            AND index_name = 'uk_note_notebook_slug'
            """,
            Long.class);
    assertTrue(
        folderUk != null && folderUk >= 1L,
        "Run backend/gradlew -p backend migrateTestDB so V300000151 applies (uk_folder_notebook_slug).");
    assertTrue(
        noteUk != null && noteUk >= 1L,
        "Run backend/gradlew -p backend migrateTestDB so V300000151 applies (uk_note_notebook_slug).");
  }

  @Test
  void duplicateFolderSlugInSameNotebook_isRejected() {
    Notebook notebook = makeMe.aNotebook().please();
    Folder first = makeMe.aFolder().notebook(notebook).name("Alpha").please();
    makeMe.entityPersister.flush();

    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbcTemplate.update(
                """
                INSERT INTO folder (notebook_id, parent_folder_id, name, slug, created_at, updated_at)
                VALUES (?, NULL, ?, ?, NOW(3), NOW(3))
                """,
                notebook.getId(),
                "Other",
                first.getSlug()));
  }

  @Test
  void duplicateNoteSlugInSameNotebook_isRejected() {
    Note root = makeMe.aNote().title("Head").please();
    Note a = makeMe.aNote().under(root).title("SlugDupA").please();
    Note b = makeMe.aNote().under(root).title("SlugDupB").please();
    makeMe.entityPersister.flush();

    assertThrows(
        DataIntegrityViolationException.class,
        () -> jdbcTemplate.update("UPDATE note SET slug = ? WHERE id = ?", a.getSlug(), b.getId()));
  }
}
