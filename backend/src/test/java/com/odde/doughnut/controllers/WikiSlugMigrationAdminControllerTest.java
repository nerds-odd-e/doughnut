package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.WikiSlugMigrationBatchResult;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WikiSlugMigrationAdminControllerTest extends ControllerTestBase {

  @Autowired WikiSlugMigrationAdminController controller;

  @Nested
  class FolderBatchMigration {
    @Test
    void folderBatchIsNoOpWhenNothingMissing() throws UnexpectedNoAccessRightException {
      currentUser.setUser(makeMe.anAdmin().please());
      User user = makeMe.aUser().please();
      makeMe.aNote().creatorAndOwner(user).please();

      WikiSlugMigrationBatchResult result = controller.batchMigrateFolders(10);

      assertThat(result.getProcessedInBatch(), equalTo(0L));
      assertThat(result.getStatus().getFoldersMissingSlug(), equalTo(0L));
    }
  }

  @Nested
  class NoteBatchMigration {
    @Test
    void noteBatchIsNoOpWhenNothingMissing() throws UnexpectedNoAccessRightException {
      currentUser.setUser(makeMe.anAdmin().please());
      User user = makeMe.aUser().please();
      Note head = makeMe.aNote().creatorAndOwner(user).please();
      head.setSlug("head");
      makeMe.entityPersister.save(head);

      WikiSlugMigrationBatchResult result = controller.batchMigrateNotes(10);

      assertThat(result.getProcessedInBatch(), equalTo(0L));
      assertThat(result.getStatus().getNotesMissingSlug(), equalTo(0L));
    }

    @Test
    void softDeletedNotesAreExcluded() throws UnexpectedNoAccessRightException {
      currentUser.setUser(makeMe.anAdmin().please());
      User user = makeMe.aUser().please();
      Note head = makeMe.aNote().creatorAndOwner(user).please();
      head.setSlug("head");
      makeMe.entityPersister.save(head);
      Note gone = makeMe.aNote().title("Gone").under(head).please();
      gone.setDeletedAt(new Timestamp(System.currentTimeMillis()));
      makeMe.entityPersister.save(gone);

      WikiSlugMigrationBatchResult result = controller.batchMigrateNotes(10);

      assertThat(result.getProcessedInBatch(), equalTo(0L));
    }
  }

  @Test
  void nonAdminCannotBatchMigrateFolders() {
    currentUser.setUser(makeMe.aUser().please());
    assertThrows(UnexpectedNoAccessRightException.class, () -> controller.batchMigrateFolders(10));
  }

  @Test
  void nonAdminCannotBatchMigrateNotes() {
    currentUser.setUser(makeMe.aUser().please());
    assertThrows(UnexpectedNoAccessRightException.class, () -> controller.batchMigrateNotes(10));
  }
}
