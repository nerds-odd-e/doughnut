package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.WikiSlugMigrationStatus;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WikiSlugMigrationAdminControllerTest extends ControllerTestBase {

  @Autowired WikiSlugMigrationAdminController controller;
  @Autowired FolderRepository folderRepository;
  @Autowired NoteRepository noteRepository;

  @Test
  void nonAdminCannotAccessStatus() {
    currentUser.setUser(makeMe.aUser().please());
    assertThrows(UnexpectedNoAccessRightException.class, () -> controller.getStatus());
  }

  @Nested
  class AsAdmin {
    @Test
    void statusMatchesRepositoryCounts() throws UnexpectedNoAccessRightException {
      User admin = makeMe.anAdmin().please();
      currentUser.setUser(admin);
      WikiSlugMigrationStatus status = controller.getStatus();
      assertThat(
          status.getFoldersMissingSlug(), equalTo(folderRepository.countFoldersMissingSlug()));
      assertThat(status.getNotesMissingSlug(), equalTo(noteRepository.countNotesMissingSlug()));
    }
  }
}
