package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.entities.Notebook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class FolderNotebookParentNameUniquePersistenceTest extends ControllerTestBase {

  @Autowired JdbcTemplate jdbcTemplate;

  @BeforeEach
  void requireParentNameUniqueIndex() {
    Long count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'folder'
            AND index_name = 'uk_folder_notebook_parent_name'
            """,
            Long.class);
    assertTrue(
        count != null && count >= 1L,
        "Run backend/gradlew -p backend migrateTestDB so V300000164 applies.");
  }

  @Test
  void duplicateSiblingFolderNameInSameNotebook_isRejected() {
    Notebook notebook = makeMe.aNotebook().please();
    makeMe.entityPersister.flush();

    jdbcTemplate.update(
        """
        INSERT INTO folder (notebook_id, parent_folder_id, name, created_at, updated_at)
        VALUES (?, NULL, 'Alpha', NOW(3), NOW(3))
        """,
        notebook.getId());

    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbcTemplate.update(
                """
                INSERT INTO folder (notebook_id, parent_folder_id, name, created_at, updated_at)
                VALUES (?, NULL, 'Alpha', NOW(3), NOW(3))
                """,
                notebook.getId()));
  }
}
