package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.UserForListing;
import com.odde.doughnut.controllers.dto.UserListingPage;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteCreator;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AdminUserControllerTest extends ControllerTestBase {
  @Autowired AdminUserController controller;

  @Test
  void nonAdminCannotAccessUserListing() {
    currentUser.setUser(makeMe.aUser().please());
    assertThrows(UnexpectedNoAccessRightException.class, () -> controller.listUsers(0, 10));
  }

  @Nested
  class AdminAccessUserListing {
    User admin;

    @BeforeEach
    void setup() {
      admin = makeMe.anAdmin().please();
      currentUser.setUser(admin);
    }

    @Test
    void canListUsersWithCorrectNoteCount() throws UnexpectedNoAccessRightException {
      User userWithNotes = makeMe.aUser().please();
      Note note = makeMe.aNote().please();
      makeMe.entityPersister.save(NoteCreator.forNoteAndUser(note, userWithNotes));

      UserListingPage result = controller.listUsers(0, 10);

      UserForListing userListing =
          result.getUsers().stream()
              .filter(u -> u.getId().equals(userWithNotes.getId()))
              .findFirst()
              .orElseThrow();
      assertThat(userListing.getNoteCount(), equalTo(1L));
    }

    @Test
    void canListUsersWithCorrectMemoryTrackerCountAndLastAssimilationTime()
        throws UnexpectedNoAccessRightException {
      User userWithTrackers = makeMe.aUser().please();
      Note note = makeMe.aNote().please();
      Timestamp assimilationTime = makeMe.aTimestamp().of(2025, 5).please();
      makeMe.aMemoryTrackerFor(note).by(userWithTrackers).assimilatedAt(assimilationTime).please();

      UserListingPage result = controller.listUsers(0, 10);

      UserForListing userListing =
          result.getUsers().stream()
              .filter(u -> u.getId().equals(userWithTrackers.getId()))
              .findFirst()
              .orElseThrow();
      assertThat(userListing.getMemoryTrackerCount(), equalTo(1L));
      assertThat(userListing.getLastAssimilationTime(), equalTo(assimilationTime));
    }

    @Test
    void canListUsersWithLastNoteTime() throws UnexpectedNoAccessRightException {
      User userWithNotes = makeMe.aUser().please();
      Timestamp noteTime = makeMe.aTimestamp().of(2025, 6).please();
      Note note = makeMe.aNote().createdAt(noteTime).please();
      makeMe.entityPersister.save(NoteCreator.forNoteAndUser(note, userWithNotes));

      UserListingPage result = controller.listUsers(0, 10);

      UserForListing userListing =
          result.getUsers().stream()
              .filter(u -> u.getId().equals(userWithNotes.getId()))
              .findFirst()
              .orElseThrow();
      assertThat(userListing.getLastNoteTime(), equalTo(noteTime));
    }

    @Test
    void paginationWorksCorrectly() throws UnexpectedNoAccessRightException {
      for (int i = 0; i < 3; i++) {
        makeMe.aUser().please();
      }

      UserListingPage firstPage = controller.listUsers(0, 3);
      UserListingPage secondPage = controller.listUsers(1, 3);

      assertThat(firstPage.getUsers().size(), equalTo(3));
      assertThat(firstPage.getPageIndex(), equalTo(0));
      assertThat(firstPage.getPageSize(), equalTo(3));
      assertThat(secondPage.getUsers().size(), greaterThanOrEqualTo(1));
      assertThat(secondPage.getPageIndex(), equalTo(1));
    }
  }
}
