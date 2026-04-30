package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class AdminDataMigrationControllerTest extends ControllerTestBase {

  @Autowired AdminDataMigrationController controller;
  @Autowired JdbcTemplate jdbcTemplate;

  static AdminDataMigrationStatusDTO migrateAllViaBatchedApi(
      AdminDataMigrationController adminDataMigrationController)
      throws UnexpectedNoAccessRightException {
    AdminDataMigrationStatusDTO r = adminDataMigrationController.runDataMigrationBatch();
    while (r.isMoreBatchesRemain()) {
      r = adminDataMigrationController.runDataMigrationBatch();
    }
    return r;
  }

  @Test
  void adminGetsStatus() throws UnexpectedNoAccessRightException {
    currentUser.setUser(makeMe.anAdmin().please());

    AdminDataMigrationStatusDTO status = controller.getAdminDataMigrationStatus();

    assertThat(status.getMessage(), notNullValue());
  }

  @Test
  void nonAdminCannotGetStatus() {
    currentUser.setUser(makeMe.aUser().please());

    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller.getAdminDataMigrationStatus());
  }

  @Test
  void adminRunsDataMigrationInBatchedStepsUntilComplete() throws UnexpectedNoAccessRightException {
    currentUser.setUser(makeMe.anAdmin().please());

    AdminDataMigrationStatusDTO first = controller.runDataMigrationBatch();
    assertThat(first.isMoreBatchesRemain(), equalTo(first.getBatchTotalPlanned() > 1));

    AdminDataMigrationStatusDTO run = migrateAllViaBatchedApi(controller);

    assertThat(run.isCompletedOnce(), equalTo(true));
    assertThat(run.isMoreBatchesRemain(), equalTo(false));
    assertThat(run.getMessage(), notNullValue());
    assertThat(run.getNotebookCountSlugScan(), greaterThanOrEqualTo(0L));
    assertThat(run.getCompletedBatchOrdinal(), equalTo(run.getBatchTotalPlanned()));
  }

  @Test
  void nonAdminCannotRunDataMigrationBatch() {
    currentUser.setUser(makeMe.aUser().please());

    assertThrows(UnexpectedNoAccessRightException.class, () -> controller.runDataMigrationBatch());
  }

  @Test
  void wikiIndexFolderMigration_detachesChildFoldersNotesAndDeletesObsoleteRoot()
      throws UnexpectedNoAccessRightException {
    User user = makeMe.aUser().please();
    currentUser.setUser(makeMe.anAdmin().please());

    Note index = makeMe.aRootNote("WillBecomeIndex").creatorAndOwner(user).please();
    int notebookId = index.getNotebook().getId();
    int indexId = index.getId();

    String persistedName = "AcmeCorp";
    jdbcTemplate.update("UPDATE notebook SET name = ? WHERE id = ?", persistedName, notebookId);
    jdbcTemplate.update(
        "UPDATE note SET title = 'index', slug = 'index', parent_id = NULL, folder_id = NULL"
            + " WHERE id = ?",
        indexId);

    Folder obsoleteHeadFolder =
        makeMe.aFolder().notebook(index.getNotebook()).name(persistedName).please();

    Folder nestedUnderIndex =
        makeMe
            .aFolder()
            .notebook(index.getNotebook())
            .parentFolder(obsoleteHeadFolder)
            .name("Section A")
            .please();

    Note regular = makeMe.aNote("Topic").under(index).please();
    jdbcTemplate.update(
        "UPDATE note SET folder_id = ? WHERE id = ?", obsoleteHeadFolder.getId(), regular.getId());

    Note target = makeMe.aNote("Target").under(index).please();
    jdbcTemplate.update("UPDATE note SET folder_id = NULL WHERE id = ?", target.getId());
    Note relation = makeMe.aRelation().between(index, target).please();
    jdbcTemplate.update(
        "UPDATE note SET folder_id = ? WHERE id = ?", obsoleteHeadFolder.getId(), relation.getId());

    makeMe.entityPersister.flush();

    AdminDataMigrationStatusDTO result = migrateAllViaBatchedApi(controller);

    assertThat(result.getDetachedChildFoldersFromIndexFolder(), greaterThanOrEqualTo(1));
    assertThat(result.getUpdatedNormalNotesDetachedFromIndex(), greaterThanOrEqualTo(1));
    assertThat(result.getUpdatedRelationNotesClearedFolder(), greaterThanOrEqualTo(1));

    Integer nestedParent =
        jdbcTemplate.queryForObject(
            "SELECT parent_folder_id FROM folder WHERE id = ?",
            Integer.class,
            nestedUnderIndex.getId());
    assertThat(nestedParent, equalTo((Integer) null));

    Integer regularParent =
        jdbcTemplate.queryForObject(
            "SELECT parent_id FROM note WHERE id = ?", Integer.class, regular.getId());
    Integer regularFolder =
        jdbcTemplate.queryForObject(
            "SELECT folder_id FROM note WHERE id = ?", Integer.class, regular.getId());
    assertThat(regularParent, equalTo((Integer) null));
    assertThat(regularFolder, equalTo((Integer) null));

    Integer relParent =
        jdbcTemplate.queryForObject(
            "SELECT parent_id FROM note WHERE id = ?", Integer.class, relation.getId());
    Integer relFolder =
        jdbcTemplate.queryForObject(
            "SELECT folder_id FROM note WHERE id = ?", Integer.class, relation.getId());
    assertThat(relParent, equalTo(indexId));
    assertThat(relFolder, equalTo((Integer) null));

    Integer deletedCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM folder WHERE id = ?", Integer.class, obsoleteHeadFolder.getId());
    assertThat(deletedCount, equalTo(0));

    Long totalNotes =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM note WHERE notebook_id = ? AND deleted_at IS NULL",
            Long.class,
            notebookId);
    Long distinctNoteSlugs =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(DISTINCT slug) FROM note WHERE notebook_id = ? AND deleted_at IS NULL",
            Long.class,
            notebookId);
    assertThat(totalNotes, equalTo(distinctNoteSlugs));

    Long totalFolders =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM folder WHERE notebook_id = ?", Long.class, notebookId);
    Long distinctFolderSlugs =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(DISTINCT slug) FROM folder WHERE notebook_id = ?",
            Long.class,
            notebookId);
    assertThat(totalFolders, equalTo(distinctFolderSlugs));
  }
}
