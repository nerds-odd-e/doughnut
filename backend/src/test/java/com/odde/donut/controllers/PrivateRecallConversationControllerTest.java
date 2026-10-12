package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Circle;
import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.ConversationMessage;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.testability.OpenAiResponseStreamMocker;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

class PrivateRecallConversationControllerTest extends ConversationMessageControllerTestBase {
  @Autowired UserController userController;

  enum OtherParticipant {
    AUTHOR,
    CIRCLE_MEMBER
  }

  enum Channel {
    MESSAGES,
    SEND,
    EXPORT,
    AI_REPLY,
    MARK_READ
  }

  User learner;
  User author;
  User circleMember;
  Circle circle;
  RecallPrompt prompt;
  Conversation conversation;

  @BeforeEach
  void setupPrivateHelp() throws UnexpectedNoAccessRightException {
    learner = currentUser.getUser();
    author = makeMe.aUser().please();
    circleMember = makeMe.aUser().please();
    circle = makeMe.aCircle().hasMember(author).hasMember(learner).hasMember(circleMember).please();
    Note note =
        makeMe
            .aNote()
            .notebook(makeMe.aNotebook().creatorAndOwner(author).owner(circle).please())
            .please();
    prompt =
        makeMe
            .aRecallPrompt()
            .forMemoryTracker(makeMe.aMemoryTrackerFor(note).by(learner).please())
            .withMcqForNote(note)
            .answerChoiceIndex(1)
            .please();
    conversation = controller.startConversationAboutRecallPrompt(prompt);
  }

  @Test
  void createsAndReadsHelpWithTheLearnersPersonalAudience() throws Exception {
    makeMe.refresh(conversation);

    Conversation read = controller.getConversation(conversation);
    assertThat(read.getSubject().getRecallPrompt(), equalTo(prompt));
    assertThat(read.getAccessOwnership(), equalTo(learner.getOwnership()));
  }

  @Test
  void learnerSendsAndReadsTheirReasoning() throws Exception {
    ConversationMessage message = controller.replyToConversation("Why choice 2?", conversation);

    assertThat(controller.getConversationMessages(conversation), contains(message));
  }

  @Test
  void learnerExportsTheirConversationRequest() throws Exception {
    controller.replyToConversation("My reasoning", conversation);

    assertThat(
        controller.exportConversation(conversation).get("input").toString(),
        containsString("My reasoning"));
  }

  @Test
  void learnerRequestsAndReadsAnAiReply() throws Exception {
    controller.replyToConversation("Explain this distinction", conversation);
    new OpenAiResponseStreamMocker(officialClient)
        .withMessage("The distinction")
        .mockStreamResponse();

    controller.getAiReply(conversation);
    makeMe.refresh(conversation);

    assertThat(
        controller.getConversationMessages(conversation).getLast().getMessage(),
        equalTo("The distinction"));
    controller.markConversationAsRead(conversation);
    makeMe.refresh(conversation);
    assertThat(conversation.getConversationMessages().getLast().getReadByReceiver(), equalTo(true));
  }

  @Test
  void learnerFindsHelpInTheirInbox() {
    var item = controller.getConversationsOfCurrentUser().getFirst();

    assertThat(item.id(), equalTo(conversation.getId()));
  }

  @Test
  void authorCannotReadPrivateHelp() {
    currentUser.setUser(author);

    UnexpectedNoAccessRightException exception =
        assertThrows(
            UnexpectedNoAccessRightException.class, () -> controller.getConversation(conversation));

    assertThat(exception.getMessage(), equalTo("It seems you cannot access this page."));
  }

  @Test
  void circleMemberCannotReadPrivateHelp() {
    currentUser.setUser(circleMember);

    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller.getConversation(conversation));
  }

  static Stream<Arguments> privateChannels() {
    return Arrays.stream(OtherParticipant.values())
        .flatMap(
            participant ->
                Arrays.stream(Channel.values()).map(channel -> Arguments.of(participant, channel)));
  }

  @ParameterizedTest
  @MethodSource("privateChannels")
  void notebookParticipantsCannotUsePrivateChannels(OtherParticipant participant, Channel channel) {
    currentUser.setUser(participant == OtherParticipant.AUTHOR ? author : circleMember);

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> {
          switch (channel) {
            case MESSAGES -> controller.getConversationMessages(conversation);
            case SEND -> controller.replyToConversation("Hi", conversation);
            case EXPORT -> controller.exportConversation(conversation);
            case AI_REPLY -> controller.getAiReply(conversation);
            case MARK_READ -> controller.markConversationAsRead(conversation);
          }
        });
  }

  @ParameterizedTest
  @EnumSource(OtherParticipant.class)
  void notebookParticipantsCannotDiscoverPrivateHelp(OtherParticipant participant) {
    currentUser.setUser(participant == OtherParticipant.AUTHOR ? author : circleMember);

    assertThat(controller.getConversationsOfCurrentUser(), empty());
  }

  @ParameterizedTest
  @EnumSource(OtherParticipant.class)
  void notebookParticipantsCannotDiscoverPrivateReasoningMessages(OtherParticipant participant)
      throws Exception {
    controller.replyToConversation("My private reasoning", conversation);
    currentUser.setUser(participant == OtherParticipant.AUTHOR ? author : circleMember);

    assertThat(userController.getMenuData("Asia/Tokyo").getUnreadMessages(), empty());
  }

  @ParameterizedTest
  @EnumSource(OtherParticipant.class)
  void notebookParticipantsCannotCopyAnotherLearnersAttempt(OtherParticipant participant) {
    currentUser.setUser(participant == OtherParticipant.AUTHOR ? author : circleMember);

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.startConversationAboutRecallPrompt(prompt));
  }

  @ParameterizedTest
  @EnumSource(OtherParticipant.class)
  void legacySharedHelpKeepsItsParticipants(OtherParticipant participant) throws Exception {
    Conversation shared = makeMe.aConversation().forARecallPrompt(prompt).from(learner).please();
    currentUser.setUser(participant == OtherParticipant.AUTHOR ? author : circleMember);

    assertThat(controller.getConversation(shared), equalTo(shared));
    assertThat(
        controller.replyToConversation("Shared discussion", shared).getSender(),
        equalTo(currentUser.getUser()));
  }
}
