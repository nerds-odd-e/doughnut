package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.MenuDataDTO;
import com.odde.doughnut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.NoteService;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

class UserMenuDataControllerTest extends ControllerTestBase {
  @Autowired UserController controller;
  @Autowired NoteService noteService;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void returnsAssimilationCountsForLoggedInUser() {
    makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();

    MenuDataDTO menuData = controller.getMenuData("Asia/Shanghai");

    assertThat(menuData.getAssimilationCount().getDueCount(), equalTo(1));
  }

  @Test
  void requiresLogin() {
    currentUser.setUser(null);
    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> controller.getMenuData("Asia/Shanghai"));
    assertEquals(HttpStatusCode.valueOf(401), exception.getStatusCode());
  }

  @Test
  void returnsCorrectRecallWindowEndTime() {
    Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
    testabilitySettings.timeTravelTo(currentTime);

    MenuDataDTO menuData = controller.getMenuData("Asia/Shanghai");

    Timestamp expectedEndAt = TimestampOperations.addHoursToTimestamp(currentTime, 4);
    assertEquals(expectedEndAt, menuData.getRecallStatus().getCurrentRecallWindowEndAt());
  }

  @Test
  void excludesMemoryTrackersForDeletedNotesFromOverview() {
    Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
    testabilitySettings.timeTravelTo(currentTime);
    Note activeNote = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    Note deletedNote = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    makeMe.aMemoryTrackerFor(activeNote).please();
    makeMe.aMemoryTrackerFor(deletedNote).please();

    noteService.destroy(
        deletedNote, NoteDeleteReferenceHandling.LEAVE_DEAD_LINKS, currentUser.getUser());

    MenuDataDTO menuData = controller.getMenuData("Asia/Shanghai");

    assertEquals(1, menuData.getRecallStatus().totalAssimilatedCount);
  }

  @Test
  void unreadCountIsOnePerUnreadMessageFromOthers() {
    Conversation conversation = makeMe.aConversation().from(currentUser.getUser()).please();
    makeMe.aConversationMessage(conversation).sender(currentUser.getUser()).please();
    makeMe.aConversationMessage(conversation).sender(makeMe.aUser().please()).please();

    assertEquals(1, controller.getMenuData("Asia/Shanghai").getUnreadMessages().size());
  }

  @Test
  void countsMessagesInsteadOfConversations() {
    Conversation conversation = makeMe.aConversation().from(currentUser.getUser()).please();
    User sender = makeMe.aUser().please();
    makeMe.aConversationMessage(conversation).sender(sender).please();
    makeMe.aConversationMessage(conversation).sender(sender).please();
    makeMe.aConversationMessage(conversation).sender(sender).please();

    assertEquals(3, controller.getMenuData("Asia/Shanghai").getUnreadMessages().size());
  }

  @Test
  void zeroUnreadWhenOnlyOwnMessages() {
    Conversation conversation = makeMe.aConversation().from(currentUser.getUser()).please();
    makeMe.aConversationMessage(conversation).sender(currentUser.getUser()).please();

    assertEquals(0, controller.getMenuData("Asia/Shanghai").getUnreadMessages().size());
  }

  @Test
  void zeroUnreadWhenAlreadyReadByReceiver() {
    Conversation conversation = makeMe.aConversation().from(currentUser.getUser()).please();
    makeMe
        .aConversationMessage(conversation)
        .sender(makeMe.aUser().please())
        .readByReceiver()
        .please();

    assertEquals(0, controller.getMenuData("Asia/Shanghai").getUnreadMessages().size());
  }
}
