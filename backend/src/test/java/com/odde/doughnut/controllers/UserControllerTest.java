package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.GeneratedTokenDTO;
import com.odde.doughnut.controllers.dto.MenuDataDTO;
import com.odde.doughnut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.doughnut.controllers.dto.RecallStatsDTO;
import com.odde.doughnut.controllers.dto.TokenConfigDTO;
import com.odde.doughnut.controllers.dto.UserDTO;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.UserToken;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.NoteService;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

class UserControllerTest extends ControllerTestBase {
  @Autowired UserController controller;
  @Autowired NoteService noteService;

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
  void newUserHealthRemoveEmptyFoldersDefaultIsFalse() {
    assertThat(controller.getUserProfile().getHealthRemoveEmptyFoldersDefault(), equalTo(false));
  }

  @Test
  void updateUserPersistsHealthRemoveEmptyFoldersDefault() throws UnexpectedNoAccessRightException {
    UserDTO dto = new UserDTO();
    dto.setName(currentUser.getUser().getName());
    dto.setSpaceIntervals(currentUser.getUser().getSpaceIntervals());
    dto.setDailyAssimilationCount(currentUser.getUser().getDailyAssimilationCount());
    dto.setHealthRemoveEmptyFoldersDefault(true);

    User response = controller.updateUser(currentUser.getUser(), dto);
    assertThat(response.getHealthRemoveEmptyFoldersDefault(), equalTo(true));
    assertThat(controller.getUserProfile().getHealthRemoveEmptyFoldersDefault(), equalTo(true));
  }

  @Test
  void generateTokenShouldReturnValidUserToken() {
    TokenConfigDTO tokenConfig = new TokenConfigDTO();
    tokenConfig.setLabel("TEST_LABEL");
    GeneratedTokenDTO generated = controller.generateToken(tokenConfig);

    assertThat(generated.label(), equalTo("TEST_LABEL"));
    assertThat(generated.token().length(), equalTo(36));
  }

  @Test
  void getTokenInfoShouldReturnTokenLabel() {
    TokenConfigDTO tokenConfig = new TokenConfigDTO();
    tokenConfig.setLabel("My Token");
    GeneratedTokenDTO generated = controller.generateToken(tokenConfig);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + generated.token());
    UserToken tokenInfo = controller.getTokenInfo(request);

    assertThat(tokenInfo.getLabel(), equalTo("My Token"));
  }

  @Test
  void getTokenInfoShouldAcceptTestAccessTokenForExistingUser() {
    User user = makeMe.aUser().please();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer access-token-of-" + user.getExternalIdentifier());
    UserToken tokenInfo = controller.getTokenInfo(request);

    assertThat(tokenInfo.getId(), equalTo(0));
    assertThat(tokenInfo.getUserId(), equalTo(user.getId()));
    assertThat(
        tokenInfo.getLabel(), equalTo("Test access token (" + user.getExternalIdentifier() + ")"));
  }

  @Test
  void getTokenInfoShouldReturn401ForInvalidToken() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer invalid-token");
    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> controller.getTokenInfo(request));
    assertEquals(HttpStatusCode.valueOf(401), exception.getStatusCode());
  }

  @Test
  void getTokenInfoShouldReturn401WhenNoAuthHeader() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> controller.getTokenInfo(request));
    assertEquals(HttpStatusCode.valueOf(401), exception.getStatusCode());
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
  void revokeTokenDeletesTokenByBearerAuth() {
    TokenConfigDTO tokenConfig = new TokenConfigDTO();
    tokenConfig.setLabel("Revokable Token");
    GeneratedTokenDTO generated = controller.generateToken(tokenConfig);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + generated.token());
    controller.revokeToken(request);

    List<UserToken> remaining = controller.getTokens();
    assertFalse(remaining.stream().anyMatch(el -> el.getLabel().equals("Revokable Token")));
  }

  @Test
  void revokeTokenReturn401ForInvalidToken() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer invalid-token");
    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> controller.revokeToken(request));
    assertEquals(HttpStatusCode.valueOf(401), exception.getStatusCode());
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
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
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
      Note activeNote = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      Note deletedNote = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      makeMe.aMemoryTrackerFor(activeNote).by(currentUser.getUser()).please();
      makeMe.aMemoryTrackerFor(deletedNote).by(currentUser.getUser()).please();

      noteService.destroy(
          deletedNote, NoteDeleteReferenceHandling.LEAVE_DEAD_LINKS, currentUser.getUser());

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

  @Nested
  class GetRecallStats {
    @Test
    void forLoginUserOnly() {
      currentUser.setUser(null);
      ResponseStatusException exception =
          assertThrows(
              ResponseStatusException.class, () -> controller.getRecallStats("Asia/Shanghai"));
      assertEquals(HttpStatusCode.valueOf(401), exception.getStatusCode());
    }

    @Test
    void returns401WhenUserNotLoggedIn() {
      currentUser.setUser(null);
      assertThrows(ResponseStatusException.class, () -> controller.getRecallStats("Asia/Shanghai"));
    }

    @Test
    void calendarHas365ZeroFilledEntries() {
      RecallStatsDTO dto = controller.getRecallStats("Asia/Shanghai");
      assertThat(dto.getCalendar().size(), equalTo(365));
      assertThat(dto.getCalendar(), everyItem(hasProperty("count", equalTo(0))));
    }

    @Test
    void retentionTrendPresentAndRespectsGuard() {
      RecallStatsDTO dto = controller.getRecallStats("Asia/Shanghai");
      assertThat(dto.getRetentionTrend().size(), equalTo(90));
      // No data -> every day insufficient (null retentionPct)
      assertThat(dto.getRetentionTrend(), everyItem(hasProperty("retentionPct", nullValue())));
    }

    @Test
    void totalsRetentionPct365IsPresent() {
      RecallStatsDTO dto = controller.getRecallStats("Asia/Shanghai");
      assertThat(dto.getTotals(), notNullValue());
      // No data -> retentionPct365 null (no answered reviews), but the field is present
      assertThat(dto.getTotals().getRetentionPct365(), nullValue());
    }

    @Test
    void reviewsTodayUsesTestabilityClockAndUserZoneTodayBoundary() {
      // Freeze "now" at 1989-01-01 00:00 UTC = 1989-01-01 08:00 Shanghai -> today local =
      // 1989-01-01
      Timestamp now = makeMe.aTimestamp().of(0, 0).please();
      testabilitySettings.timeTravelTo(now);

      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      MemoryTracker mt = makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).please();
      // An answer at Shanghai 1989-01-01 10:00 = UTC 1989-01-01 02:00, which is < now (00:00 UTC)?
      // now is 00:00 UTC; an answer at 02:00 UTC is AFTER now -> excluded by the < endTime bound.
      // Use an answer before now: Shanghai 1989-01-01 07:00 = UTC 1989-01-00 23:00 = 1988-12-31
      // 23:00 UTC.
      // That lands on local day 1988-12-31 (Shanghai) -> not "today". To land on today (1989-01-01
      // Shanghai)
      // AND before now (00:00 UTC), we need an answer between 1988-12-31 16:00 UTC (1989-01-01
      // 00:00 Shanghai)
      // and 1989-01-01 00:00 UTC. Use 1988-12-31 17:00 UTC = 1989-01-01 01:00 Shanghai.
      Timestamp todayAnswer =
          Timestamp.from(
              java.time.ZonedDateTime.of(1988, 12, 31, 17, 0, 0, 0, java.time.ZoneId.of("UTC"))
                  .toInstant());
      makeMe
          .aRecallPrompt()
          .withPredefinedQuestionForNote(note)
          .forMemoryTracker(mt)
          .answerChoiceIndex(0)
          .answerTimestamp(todayAnswer)
          .please();

      RecallStatsDTO dto = controller.getRecallStats("Asia/Shanghai");

      assertThat(dto.getTotals().getReviewsToday(), equalTo(1));
    }

    @Test
    void scopedToCurrentUserExcludesOtherUsersPrompts() {
      Timestamp now = makeMe.aTimestamp().of(10, 12).please();
      testabilitySettings.timeTravelTo(now);

      Note myNote = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      MemoryTracker myMt = makeMe.aMemoryTrackerFor(myNote).by(currentUser.getUser()).please();
      Timestamp myAnswer = makeMe.aTimestamp().of(9, 10).please();
      makeMe
          .aRecallPrompt()
          .withPredefinedQuestionForNote(myNote)
          .forMemoryTracker(myMt)
          .answerChoiceIndex(0)
          .answerTimestamp(myAnswer)
          .please();

      User other = makeMe.aUser().please();
      Note otherNote = makeMe.aNote().notebookOwnedBy(other).please();
      MemoryTracker otherMt = makeMe.aMemoryTrackerFor(otherNote).by(other).please();
      Timestamp otherAnswer = makeMe.aTimestamp().of(9, 10).please();
      makeMe
          .aRecallPrompt()
          .withPredefinedQuestionForNote(otherNote)
          .forMemoryTracker(otherMt)
          .answerChoiceIndex(0)
          .answerTimestamp(otherAnswer)
          .please();

      RecallStatsDTO dto = controller.getRecallStats("UTC");

      // Only my 1 review counted in the 365d window
      assertThat(dto.getTotals().getTotalReviews365(), equalTo(1));
    }
  }
}
