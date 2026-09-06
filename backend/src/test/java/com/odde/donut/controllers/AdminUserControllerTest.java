package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.UserForListing;
import com.odde.donut.controllers.dto.UserListingPage;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.NoteCreator;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
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
    @BeforeEach
    void setup() {
      currentUser.setUser(makeMe.anAdmin().please());
    }

    @Test
    void listsMemoryTrackerCountAndLastAssimilationTime() throws UnexpectedNoAccessRightException {
      User userWithTrackers = makeMe.aUser().please();
      Note note = makeMe.aNote().please();
      Timestamp assimilationTime = makeMe.aTimestamp().of(2025, 5).please();
      makeMe.aMemoryTrackerFor(note).by(userWithTrackers).assimilatedAt(assimilationTime).please();

      UserForListing userListing = listingFor(userWithTrackers);

      assertThat(userListing.getMemoryTrackerCount(), equalTo(1L));
      assertThat(userListing.getLastAssimilationTime(), equalTo(assimilationTime));
    }

    @Test
    void listsNoteCountAndLastNoteTime() throws UnexpectedNoAccessRightException {
      User userWithNotes = makeMe.aUser().please();
      Timestamp noteTime = makeMe.aTimestamp().of(2025, 6).please();
      Note note = makeMe.aNote().createdAt(noteTime).please();
      makeMe.entityPersister.save(NoteCreator.forNoteAndUser(note, userWithNotes));

      UserForListing userListing = listingFor(userWithNotes);

      assertThat(userListing.getNoteCount(), equalTo(1L));
      assertThat(userListing.getLastNoteTime(), equalTo(noteTime));
    }

    @Test
    void paginatesUsers() throws UnexpectedNoAccessRightException {
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

    @Test
    void findsUserListingWhenTargetIsBeyondFirstPage() throws UnexpectedNoAccessRightException {
      for (int i = 0; i < 10; i++) {
        makeMe.aUser("aaa-filler-" + i).please();
      }
      User target = makeMe.aUser("zzz-target").please();

      UserListingPage firstPage = controller.listUsers(0, 10);
      assertThat(
          "target must be beyond page 0 to prove the lookup",
          firstPage.getUsers().stream().noneMatch(u -> u.getId().equals(target.getId())),
          equalTo(true));

      UserForListing userListing = listingFor(target);

      assertThat(userListing.getId(), equalTo(target.getId()));
    }

    private UserForListing listingFor(User user) throws UnexpectedNoAccessRightException {
      int pageSize = 10;
      UserListingPage firstPage = controller.listUsers(0, pageSize);
      int totalPages = firstPage.getTotalPages();
      for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
        UserListingPage page =
            pageIndex == 0 ? firstPage : controller.listUsers(pageIndex, pageSize);
        for (UserForListing candidate : page.getUsers()) {
          if (candidate.getId().equals(user.getId())) {
            return candidate;
          }
        }
      }
      throw new AssertionError(
          "Expected user id %s across %d pages (totalCount %s)."
              .formatted(user.getId(), totalPages, firstPage.getTotalCount()));
    }
  }
}
