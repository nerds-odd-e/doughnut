package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class HeadNoteToIndexMigrationPersistenceTest extends ControllerTestBase {

  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired NotebookController notebookController;
  @Autowired EntityManager entityManager;

  private static final String BACKFILL_NOTEBOOK_NAME =
      """
      UPDATE notebook n
      INNER JOIN note hn ON hn.notebook_id = n.id AND hn.deleted_at IS NULL
      SET n.name = LEFT(hn.title, 150)
      WHERE hn.id = ?
      """;

  private static final String DISAMBIGUATE_NON_HEAD_INDEX_SLUGS =
      """
      UPDATE note n
      SET n.slug = CONCAT('legacy-index-', n.id)
      WHERE n.id <> ?
        AND n.notebook_id = ?
        AND n.slug = 'index'
        AND n.deleted_at IS NULL
      """;

  private static final String HEAD_NOTES_TO_INDEX =
      """
      UPDATE note hn
      SET hn.title = 'index',
          hn.slug = 'index',
          hn.folder_id = NULL,
          hn.parent_id = NULL
      WHERE hn.id = ?
        AND hn.deleted_at IS NULL
      """;

  @Test
  void notebookTableHasNameColumn() {
    Long count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'notebook'
            AND column_name = 'name'
            """,
            Long.class);
    assertTrue(
        count != null && count >= 1L,
        "Run backend migration so V300000154 applies (notebook.name).");
  }

  @Test
  void headToIndexMigration_preservesNotebookNameAndResolvesIndexSlug()
      throws UnexpectedNoAccessRightException {
    User user = makeMe.aUser().please();
    currentUser.setUser(user);
    Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
    Note legacyRoot =
        makeMe
            .aNote("LegacyCap")
            .inNotebook(notebook)
            .creatorAndOwner(user)
            .details("Notebook body")
            .please();
    int headId = legacyRoot.getId();
    int notebookId = notebook.getId();
    makeMe.entityPersister.flush();

    jdbcTemplate.update(BACKFILL_NOTEBOOK_NAME, headId);
    jdbcTemplate.update(DISAMBIGUATE_NON_HEAD_INDEX_SLUGS, headId, notebookId);
    jdbcTemplate.update(HEAD_NOTES_TO_INDEX, headId);
    entityManager.clear();

    String storedName =
        jdbcTemplate.queryForObject(
            "SELECT name FROM notebook WHERE id = ?", String.class, notebookId);
    assertThat(storedName, equalTo("LegacyCap"));

    String storedTitle =
        jdbcTemplate.queryForObject("SELECT title FROM note WHERE id = ?", String.class, headId);
    String storedSlug =
        jdbcTemplate.queryForObject("SELECT slug FROM note WHERE id = ?", String.class, headId);
    assertThat(storedTitle, equalTo("index"));
    assertThat(storedSlug, equalTo("index"));

    NoteRealm realm = notebookController.getNoteBySlug(notebook, "index");
    assertThat(realm.getId(), equalTo(headId));
    assertThat(realm.getNote().getDetails(), equalTo("Notebook body"));
  }
}
