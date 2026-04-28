package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.WikiSlugMigrationBatchResult;
import com.odde.doughnut.controllers.dto.WikiSlugMigrationStatus;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class WikiSlugMigrationAdminControllerTest extends ControllerTestBase {

  @Autowired WikiSlugMigrationAdminController controller;
  @Autowired FolderRepository folderRepository;
  @Autowired NoteRepository noteRepository;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired EntityManager entityManager;

  @Nested
  class FolderBatchMigration {
    @Test
    void migratesRootFoldersWithSiblingCollision() throws UnexpectedNoAccessRightException {
      currentUser.setUser(makeMe.anAdmin().please());
      User user = makeMe.aUser().please();
      Note head = makeMe.aNote().creatorAndOwner(user).please();
      Notebook notebook = head.getNotebook();
      makeMe.aFolder().notebook(notebook).name("Same Name").please();
      makeMe.aFolder().notebook(notebook).name("Same Name").please();

      WikiSlugMigrationBatchResult result = controller.batchMigrateFolders(10);

      assertThat(result.getProcessedInBatch(), equalTo(2L));
      assertThat(result.getStatus().getFoldersMissingSlug(), equalTo(0L));
      assertThat(folderRepository.countFoldersMissingSlug(), equalTo(0L));
    }

    @Test
    void nestedChildWaitsUntilParentHasSlug() throws UnexpectedNoAccessRightException {
      currentUser.setUser(makeMe.anAdmin().please());
      User user = makeMe.aUser().please();
      Note head = makeMe.aNote().creatorAndOwner(user).please();
      Notebook notebook = head.getNotebook();
      Folder parent = makeMe.aFolder().notebook(notebook).name("Parent").please();
      Folder child =
          makeMe.aFolder().notebook(notebook).parentFolder(parent).name("Child").please();

      WikiSlugMigrationBatchResult first = controller.batchMigrateFolders(10);
      assertThat(first.getProcessedInBatch(), equalTo(1L));
      assertThat(first.getStatus().getFoldersMissingSlug(), equalTo(1L));

      Folder reloadedParent = folderRepository.findById(parent.getId()).orElseThrow();
      assertThat(reloadedParent.getSlug(), equalTo("parent"));

      WikiSlugMigrationBatchResult second = controller.batchMigrateFolders(10);
      assertThat(second.getProcessedInBatch(), equalTo(1L));
      Folder reloadedChild = folderRepository.findById(child.getId()).orElseThrow();
      assertThat(reloadedChild.getSlug(), equalTo("parent/child"));
      assertThat(second.getStatus().getFoldersMissingSlug(), equalTo(0L));
    }

    @Test
    void twoSiblingsWithSameNameInOneBatchGetSuffixes() throws UnexpectedNoAccessRightException {
      currentUser.setUser(makeMe.anAdmin().please());
      User user = makeMe.aUser().please();
      Note head = makeMe.aNote().creatorAndOwner(user).please();
      Notebook notebook = head.getNotebook();
      Folder parent = makeMe.aFolder().notebook(notebook).name("P").please();
      folderRepository.save(parent);
      parent.setSlug("p");
      folderRepository.save(parent);
      makeMe.aFolder().notebook(notebook).parentFolder(parent).name("Dup").please();
      makeMe.aFolder().notebook(notebook).parentFolder(parent).name("Dup").please();

      WikiSlugMigrationBatchResult result = controller.batchMigrateFolders(10);

      assertThat(result.getProcessedInBatch(), equalTo(2L));
    }

    @Test
    void repeatedSmallBatchesDrainAllRootFolders() throws UnexpectedNoAccessRightException {
      currentUser.setUser(makeMe.anAdmin().please());
      User user = makeMe.aUser().please();
      Note head = makeMe.aNote().creatorAndOwner(user).please();
      Notebook notebook = head.getNotebook();
      makeMe.aFolder().notebook(notebook).name("One").please();
      makeMe.aFolder().notebook(notebook).name("Two").please();
      makeMe.aFolder().notebook(notebook).name("Three").please();
      makeMe.aFolder().notebook(notebook).name("Four").please();
      makeMe.aFolder().notebook(notebook).name("Five").please();

      int batchLimit = 2;
      while (folderRepository.countFoldersMissingSlug() > 0) {
        long missingBefore = folderRepository.countFoldersMissingSlug();
        WikiSlugMigrationBatchResult result = controller.batchMigrateFolders(batchLimit);
        long missingAfterRepo = folderRepository.countFoldersMissingSlug();
        assertThat(missingAfterRepo, lessThanOrEqualTo(missingBefore));
        assertThat(result.getStatus().getFoldersMissingSlug(), equalTo(missingAfterRepo));
        WikiSlugMigrationStatus fromGetStatus = controller.getStatus();
        assertThat(fromGetStatus.getFoldersMissingSlug(), equalTo(missingAfterRepo));
        assertThat(missingBefore - missingAfterRepo, equalTo(result.getProcessedInBatch()));
      }

      WikiSlugMigrationBatchResult noOp = controller.batchMigrateFolders(batchLimit);
      assertThat(noOp.getProcessedInBatch(), equalTo(0L));
      assertThat(noOp.getStatus().getFoldersMissingSlug(), equalTo(0L));
    }

    @Test
    void folderBatchIsNoOpWhenNothingMissing() throws UnexpectedNoAccessRightException {
      currentUser.setUser(makeMe.anAdmin().please());
      User user = makeMe.aUser().please();
      makeMe.aNote().creatorAndOwner(user).please();

      WikiSlugMigrationBatchResult result = controller.batchMigrateFolders(10);

      assertThat(result.getProcessedInBatch(), equalTo(0L));
      assertThat(result.getStatus().getFoldersMissingSlug(), equalTo(0L));
    }

    @Test
    void migratesFolderWithLegacyEmptyName() throws UnexpectedNoAccessRightException {
      currentUser.setUser(makeMe.anAdmin().please());
      User user = makeMe.aUser().please();
      Note head = makeMe.aNote().creatorAndOwner(user).please();
      Notebook notebook = head.getNotebook();
      Folder folder = makeMe.aFolder().notebook(notebook).name("placeholder").please();
      int id = folder.getId();
      jdbcTemplate.update("UPDATE folder SET name = '' WHERE id = ?", id);
      entityManager.clear();

      WikiSlugMigrationBatchResult result = controller.batchMigrateFolders(10);

      assertThat(result.getProcessedInBatch(), equalTo(1L));
      Folder reloaded = folderRepository.findById(id).orElseThrow();
      assertThat(reloaded.getName(), equalTo("Untitled"));
      assertThat(reloaded.getSlug(), equalTo("untitled"));
    }
  }

  @Nested
  class NoteBatchMigration {
    @Test
    void migratesRootNotesWithCollision() throws UnexpectedNoAccessRightException {
      currentUser.setUser(makeMe.anAdmin().please());
      User user = makeMe.aUser().please();
      Note head = makeMe.aNote().creatorAndOwner(user).please();
      head.setSlug("head");
      noteRepository.save(head);
      makeMe.aNote().title("Hello").under(head).please();
      makeMe.aNote().title("Hello").under(head).please();

      WikiSlugMigrationBatchResult result = controller.batchMigrateNotes(10);

      assertThat(result.getProcessedInBatch(), equalTo(2L));
      assertThat(result.getStatus().getNotesMissingSlug(), equalTo(0L));
    }

    @Test
    void folderedNoteUsesFolderSlugPrefix() throws UnexpectedNoAccessRightException {
      currentUser.setUser(makeMe.anAdmin().please());
      User user = makeMe.aUser().please();
      Note head = makeMe.aNote().creatorAndOwner(user).please();
      head.setSlug("head");
      noteRepository.save(head);
      Notebook notebook = head.getNotebook();
      Folder folder = makeMe.aFolder().notebook(notebook).name("Docs").please();
      folder.setSlug("docs");
      folderRepository.save(folder);
      makeMe.aNote().title("Readme").under(head).folder(folder).please();

      WikiSlugMigrationBatchResult result = controller.batchMigrateNotes(10);

      assertThat(result.getProcessedInBatch(), equalTo(1L));
      Note note = noteRepository.searchExactInNotebook(notebook.getId(), "Readme").getFirst();
      assertThat(note.getSlug(), equalTo("docs/readme"));
    }

    @Test
    void sameTitleInDifferentFoldersGetsDistinctPaths() throws UnexpectedNoAccessRightException {
      currentUser.setUser(makeMe.anAdmin().please());
      User user = makeMe.aUser().please();
      Note head = makeMe.aNote().creatorAndOwner(user).please();
      head.setSlug("head");
      noteRepository.save(head);
      Notebook notebook = head.getNotebook();
      Folder a = makeMe.aFolder().notebook(notebook).name("A").please();
      a.setSlug("a");
      folderRepository.save(a);
      Folder b = makeMe.aFolder().notebook(notebook).name("B").please();
      b.setSlug("b");
      folderRepository.save(b);
      makeMe.aNote().title("T").under(head).folder(a).please();
      makeMe.aNote().title("T").under(head).folder(b).please();

      WikiSlugMigrationBatchResult result = controller.batchMigrateNotes(10);

      assertThat(result.getProcessedInBatch(), equalTo(2L));
    }

    @Test
    void nestedFolderNoteGetsFullPath() throws UnexpectedNoAccessRightException {
      currentUser.setUser(makeMe.anAdmin().please());
      User user = makeMe.aUser().please();
      Note head = makeMe.aNote().creatorAndOwner(user).please();
      head.setSlug("head");
      noteRepository.save(head);
      Notebook notebook = head.getNotebook();
      Folder parent = makeMe.aFolder().notebook(notebook).name("Alpha").please();
      parent.setSlug("alpha");
      folderRepository.save(parent);
      Folder child = makeMe.aFolder().notebook(notebook).parentFolder(parent).name("Beta").please();
      child.setSlug("alpha/beta");
      folderRepository.save(child);
      makeMe.aNote().title("Leaf").under(head).folder(child).please();

      WikiSlugMigrationBatchResult result = controller.batchMigrateNotes(10);

      assertThat(result.getProcessedInBatch(), equalTo(1L));
      Note note = noteRepository.searchExactInNotebook(notebook.getId(), "Leaf").getFirst();
      assertThat(note.getSlug(), equalTo("alpha/beta/leaf"));
    }

    @Test
    void skipsNoteWhenFolderSlugMissing() throws UnexpectedNoAccessRightException {
      currentUser.setUser(makeMe.anAdmin().please());
      User user = makeMe.aUser().please();
      Note head = makeMe.aNote().creatorAndOwner(user).please();
      head.setSlug("head");
      noteRepository.save(head);
      Notebook notebook = head.getNotebook();
      Folder folder = makeMe.aFolder().notebook(notebook).name("NoSlug").please();
      makeMe.aNote().title("Inside").under(head).folder(folder).please();

      WikiSlugMigrationBatchResult result = controller.batchMigrateNotes(10);

      assertThat(result.getProcessedInBatch(), equalTo(0L));
      assertThat(result.getStatus().getNotesMissingSlug(), equalTo(1L));
    }

    @Test
    void softDeletedNotesAreExcluded() throws UnexpectedNoAccessRightException {
      currentUser.setUser(makeMe.anAdmin().please());
      User user = makeMe.aUser().please();
      Note head = makeMe.aNote().creatorAndOwner(user).please();
      head.setSlug("head");
      noteRepository.save(head);
      Note gone = makeMe.aNote().title("Gone").under(head).please();
      gone.setDeletedAt(new Timestamp(System.currentTimeMillis()));
      noteRepository.save(gone);

      WikiSlugMigrationBatchResult result = controller.batchMigrateNotes(10);

      assertThat(result.getProcessedInBatch(), equalTo(0L));
    }

    @Test
    void repeatedSmallBatchesDrainAllNotesMissingSlug() throws UnexpectedNoAccessRightException {
      currentUser.setUser(makeMe.anAdmin().please());
      User user = makeMe.aUser().please();
      Note head = makeMe.aNote().creatorAndOwner(user).please();
      head.setSlug("head");
      noteRepository.save(head);
      Notebook notebook = head.getNotebook();
      Folder folder = makeMe.aFolder().notebook(notebook).name("Shelf").please();
      folder.setSlug("shelf");
      folderRepository.save(folder);
      makeMe.aNote().title("A").under(head).folder(folder).please();
      makeMe.aNote().title("B").under(head).folder(folder).please();
      makeMe.aNote().title("C").under(head).folder(folder).please();

      int batchLimit = 2;
      while (noteRepository.countNotesMissingSlug() > 0) {
        long missingBefore = noteRepository.countNotesMissingSlug();
        WikiSlugMigrationBatchResult result = controller.batchMigrateNotes(batchLimit);
        long missingAfterRepo = noteRepository.countNotesMissingSlug();
        assertThat(missingAfterRepo, lessThanOrEqualTo(missingBefore));
        assertThat(result.getStatus().getNotesMissingSlug(), equalTo(missingAfterRepo));
        WikiSlugMigrationStatus fromGetStatus = controller.getStatus();
        assertThat(fromGetStatus.getNotesMissingSlug(), equalTo(missingAfterRepo));
        assertThat(missingBefore - missingAfterRepo, equalTo(result.getProcessedInBatch()));
      }

      WikiSlugMigrationBatchResult noOp = controller.batchMigrateNotes(batchLimit);
      assertThat(noOp.getProcessedInBatch(), equalTo(0L));
      assertThat(noOp.getStatus().getNotesMissingSlug(), equalTo(0L));
    }

    @Test
    void noteBatchIsNoOpWhenNothingMissing() throws UnexpectedNoAccessRightException {
      currentUser.setUser(makeMe.anAdmin().please());
      User user = makeMe.aUser().please();
      Note head = makeMe.aNote().creatorAndOwner(user).please();
      head.setSlug("head");
      noteRepository.save(head);

      WikiSlugMigrationBatchResult result = controller.batchMigrateNotes(10);

      assertThat(result.getProcessedInBatch(), equalTo(0L));
      assertThat(result.getStatus().getNotesMissingSlug(), equalTo(0L));
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
