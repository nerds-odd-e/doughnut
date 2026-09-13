package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.controllers.currentUser.CurrentUser;
import com.odde.donut.controllers.currentUser.ThreadLocalCurrentUser;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.NoteTrashUndoDTO;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.services.notebookGit.NoteLegacyTrashMigration;
import com.odde.donut.testability.MakeMe;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies that a legacy soft-deleted note migrated by {@link NoteLegacyTrashMigration} is visible
 * through {@link NoteController#showNote} and recoverable through {@link
 * NoteController#undoTrashNote} (Move to an active folder or notebook root). Runs the migration
 * with no ambient Spring transaction (NOT_SUPPORTED), then exercises the controller methods which
 * each open their own transaction.
 */
@SpringBootTest
@ActiveProfiles("test")
class NoteLegacyTrashMigrationControllerTest {

  @Autowired MakeMe makeMe;
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired NoteController noteController;
  @Autowired NoteRepository noteRepository;

  @TestBean CurrentUser currentUser;

  static CurrentUser currentUser() {
    return new ThreadLocalCurrentUser();
  }

  private final List<Integer> ownerUserIdsToClean = new ArrayList<>();

  @AfterEach
  void cleanUp() {
    for (Integer ownerUserId : ownerUserIdsToClean) {
      jdbcTemplate.update(
          "DELETE FROM memory_tracker WHERE note_id IN "
              + "(SELECT id FROM note WHERE notebook_id IN "
              + "(SELECT id FROM notebook WHERE creator_id = ?))",
          ownerUserId);
      jdbcTemplate.update(
          "DELETE FROM note WHERE notebook_id IN "
              + "(SELECT id FROM notebook WHERE creator_id = ?)",
          ownerUserId);
      jdbcTemplate.update(
          "DELETE FROM folder WHERE notebook_id IN "
              + "(SELECT id FROM notebook WHERE creator_id = ?)",
          ownerUserId);
      jdbcTemplate.update("DELETE FROM notebook WHERE creator_id = ?", ownerUserId);
      jdbcTemplate.update("DELETE FROM user WHERE id = ?", ownerUserId);
    }
    ownerUserIdsToClean.clear();
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void migratedNoteIsVisibleThroughShowNoteAndRecoverableByMoveToNotebookRoot() throws Exception {
    User owner = makeMe.aUser().please();
    ownerUserIdsToClean.add(owner.getId());
    currentUser.setUser(owner);
    Notebook notebook =
        makeMe.aNotebook().creatorAndOwner(owner).readmeContent("# Notebook readme").please();
    Folder recipes = makeMe.aFolder().notebook(notebook).name("Recipes").please();
    Folder italian = makeMe.aFolder().parentFolder(recipes).name("Italian").please();
    Folder pasta = makeMe.aFolder().parentFolder(italian).name("Pasta").please();
    Note deletedLearnedNote =
        makeMe.aNote("Carbonara").folder(pasta).content("Guanciale eggs").please();
    jdbcTemplate.update(
        "INSERT INTO memory_tracker (user_id, note_id, assimilated_at, next_recall_at)"
            + " VALUES (?, ?, ?, ?)",
        owner.getId(),
        deletedLearnedNote.getId(),
        Timestamp.from(Instant.parse("2026-07-01T00:00:00Z")),
        Timestamp.from(Instant.parse("2026-07-01T00:00:00Z")));
    Integer deletedNoteId = deletedLearnedNote.getId();

    jdbcTemplate.update(
        "UPDATE note SET deleted_at = ? WHERE id = ?",
        Timestamp.from(Instant.parse("2026-08-01T00:00:00Z")),
        deletedNoteId);

    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      NoteLegacyTrashMigration.run(connection, Instant.parse("2026-09-13T12:00:00Z"));
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }

    Note migrated = noteRepository.findById(deletedNoteId).orElseThrow();

    NoteRealm shown = noteController.showNote(migrated);
    assertThat(shown.getNote().getId(), equalTo(deletedNoteId));
    assertThat(shown.getNote().getTitle(), equalTo("Carbonara"));
    assertThat(
        isFolderTrashed(
            jdbcTemplate.queryForObject(
                "SELECT folder_id FROM note WHERE id = ?", Integer.class, deletedNoteId)),
        equalTo(true));

    NoteTrashUndoDTO undo = new NoteTrashUndoDTO();
    undo.setPriorTitle("Carbonara");
    undo.setPriorFolderId(null);
    NoteRealm restored = noteController.undoTrashNote(migrated, undo);

    assertThat(restored.getNote().getId(), equalTo(deletedNoteId));
    assertThat(restored.getNote().getTitle(), equalTo("Carbonara"));
    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT folder_id FROM note WHERE id = ?", Integer.class, deletedNoteId),
        nullValue());
  }

  private Boolean isFolderTrashed(Integer folderId) {
    if (folderId == null) return false;
    return jdbcTemplate.queryForObject(
        "SELECT id IN (SELECT id FROM trashed_folder) FROM folder WHERE id = ?",
        Boolean.class,
        folderId);
  }
}
