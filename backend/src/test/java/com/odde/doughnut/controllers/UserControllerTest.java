package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.MenuDataDTO;
import com.odde.doughnut.controllers.dto.TokenConfigDTO;
import com.odde.doughnut.controllers.dto.UserDTO;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.UserToken;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

class UserControllerTest extends ControllerTestBase {
  @Autowired UserController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void createUserWhileSessionTimeout() {
    assertThrows(
        ResponseStatusException.class, () -> controller.createUser(null, currentUser.getUser()));
  }

  @Test
  void updateUserSuccessfully() throws UnexpectedNoAccessRightException {
    UserDTO dto = new UserDTO();
    dto.setName("new name");
    dto.setSpaceIntervals("1,2,3,4,5,6,7,8,9,10,11,12,13,14,15");
    dto.setDailyAssimilationCount(12);
    User response = controller.updateUser(currentUser.getUser(), dto);
    assertThat(response.getName(), equalTo(dto.getName()));
    assertThat(response.getSpaceIntervals(), equalTo(dto.getSpaceIntervals()));
    assertThat(response.getDailyAssimilationCount(), equalTo(dto.getDailyAssimilationCount()));
  }

  @Test
  void updateOtherUserProfile() {
    UserDTO dto = new UserDTO();
    dto.setName("new name");
    User anotherUser = makeMe.aUser().please();
    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller.updateUser(anotherUser, dto));
  }

  @Test
  void generateTokenShouldReturnValidUserToken() {
    TokenConfigDTO tokenConfig = new TokenConfigDTO();
    tokenConfig.setLabel("TEST_LABEL");
    UserToken userToken = controller.generateToken(tokenConfig);

    assertThat(userToken.getUserId(), equalTo(currentUser.getUser().getId()));
    assertThat(userToken.getLabel(), equalTo("TEST_LABEL"));
    assertThat(userToken.getToken().length(), equalTo(36));
  }

  @Test
  void getTokensTest() {
    UserToken userToken =
        makeMe.aUserToken().forUser(currentUser.getUser()).withLabel("TEST_LABEL").please();
    makeMe.entityPersister.save(userToken);

    List<UserToken> getTokens = controller.getTokens();

    assertTrue(getTokens.stream().anyMatch(el -> el.getLabel().equals("TEST_LABEL")));
    assertThat(getTokens.size(), equalTo(1));
  }

  @Test
  void getTokensWithMultipleTokens() {
    UserToken userToken = new UserToken(currentUser.getUser().getId(), "token", "LABEL");
    makeMe.entityPersister.save(userToken);

    List<UserToken> getTokens = controller.getTokens();

    assertTrue(getTokens.stream().anyMatch(el -> el.getLabel().equals("LABEL")));
    assertThat(getTokens.size(), equalTo(1));
  }

  @Test
  void deleteTokenTest() {
    UserToken userToken =
        makeMe.aUserToken().forUser(currentUser.getUser()).withLabel("DELETE_LABEL").please();
    makeMe.entityPersister.save(userToken);

    controller.deleteToken(userToken.getId());

    List<UserToken> getTokens = controller.getTokens();
    assertFalse(getTokens.stream().anyMatch(el -> el.getId().equals(userToken.getId())));
    assertThat(getTokens.size(), equalTo(0));
  }

  @Test
  void deleteTokenTestForAnotherUser() {
    User anotherUser = makeMe.aUser().please();
    UserToken userToken2 =
        makeMe.aUserToken().forUser(anotherUser).withLabel("OTHER_USER_TOKEN").please();
    makeMe.entityPersister.save(userToken2);

    assertThrows(ResponseStatusException.class, () -> controller.deleteToken(userToken2.getId()));
  }

  @Nested
  class GetMenuData {
    @Test
    void shouldReturnAssimilationCountsForLoggedInUser() {
      // Create a note that needs assimilation
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      assertThat(note.getId(), notNullValue());

      MenuDataDTO menuData = controller.getMenuData("Asia/Shanghai");

      assertThat(menuData.getAssimilationCount().getDueCount(), equalTo(1));
    }

    @Test
    void shouldThrowExceptionWhenUserNotLoggedIn() {
      currentUser.setUser(null);
      assertThrows(ResponseStatusException.class, () -> controller.getMenuData("Asia/Shanghai"));
    }

    @Test
    void shouldReturnCorrectRecallWindowEndTime() {
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);

      MenuDataDTO menuData = controller.getMenuData("Asia/Shanghai");

      // currentTime is 1989-01-01 00:00:00 UTC, which is 1989-01-01 08:00:00 in Asia/Shanghai
      // Since hour < 12, alignByHalfADay returns same day at 12:00:00 Asia/Shanghai = 04:00:00 UTC
      Timestamp expectedEndAt = TimestampOperations.addHoursToTimestamp(currentTime, 4);
      assertEquals(expectedEndAt, menuData.getRecallStatus().getCurrentRecallWindowEndAt());
    }

    @Test
    void shouldExcludeMemoryTrackersForDeletedNotesFromOverview() {
      Timestamp currentTime = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(currentTime);
      Note activeNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      Note deletedNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      makeMe.aMemoryTrackerFor(activeNote).by(currentUser.getUser()).please();
      makeMe.aMemoryTrackerFor(deletedNote).by(currentUser.getUser()).please();

      deletedNote.setDeletedAt(currentTime);
      makeMe.entityPersister.merge(deletedNote);

      MenuDataDTO menuData = controller.getMenuData("Asia/Shanghai");

      assertEquals(1, menuData.getRecallStatus().totalAssimilatedCount);
    }

    @Test
    void forLoginUserOnly() {
      currentUser.setUser(null);
      ResponseStatusException exception =
          assertThrows(
              ResponseStatusException.class, () -> controller.getMenuData("Asia/Shanghai"));
      assertEquals(HttpStatusCode.valueOf(401), exception.getStatusCode());
    }

    @Test
    void getOneUnreadConversationCountOfCurrentUser() {
      Conversation conversation = makeMe.aConversation().from(currentUser.getUser()).please();
      makeMe.aConversationMessage(conversation).sender(currentUser.getUser()).please();
      makeMe.aConversationMessage(conversation).sender(makeMe.aUser().please()).please();

      MenuDataDTO menuData = controller.getMenuData("Asia/Shanghai");

      assertEquals(1, menuData.getUnreadConversations().size());
    }

    @Test
    void countMessagesInsteadOfConversations() {
      Conversation conversation = makeMe.aConversation().from(currentUser.getUser()).please();
      User sender = makeMe.aUser().please();
      makeMe.aConversationMessage(conversation).sender(sender).please();
      makeMe.aConversationMessage(conversation).sender(sender).please();
      makeMe.aConversationMessage(conversation).sender(sender).please();

      MenuDataDTO menuData = controller.getMenuData("Asia/Shanghai");

      assertEquals(3, menuData.getUnreadConversations().size());
    }

    @Test
    void zeroUnreadConversationCountForSender() {
      Conversation conversation = makeMe.aConversation().from(currentUser.getUser()).please();
      makeMe.aConversationMessage(conversation).sender(currentUser.getUser()).please();

      MenuDataDTO menuData = controller.getMenuData("Asia/Shanghai");

      assertEquals(0, menuData.getUnreadConversations().size());
    }

    @Test
    void getZeroUnreadConversationWhenSenderIsCurrentUser() {
      Conversation conversation = makeMe.aConversation().from(currentUser.getUser()).please();
      makeMe
          .aConversationMessage(conversation)
          .sender(currentUser.getUser())
          .readByReceiver()
          .please();

      MenuDataDTO menuData = controller.getMenuData("Asia/Shanghai");

      assertEquals(0, menuData.getUnreadConversations().size());
    }
  }
}
