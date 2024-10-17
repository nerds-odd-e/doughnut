package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ConversationService;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestConversationMessageControllerTest {

  @Autowired ConversationService conversationService;
  @Autowired MakeMe makeMe;
  private UserModel currentUser;
  RestConversationMessageController controller;

  @Autowired ModelFactoryService modelFactoryService;
  AssessmentQuestionInstance assessmentQuestionInstance;

  @BeforeEach
  void setup() {
    currentUser = makeMe.aUser().toModelPlease();
    controller = new RestConversationMessageController(currentUser, conversationService);
    Notebook notebook = makeMe.aNotebook().please();
    AssessmentAttempt assessmentAttempt =
        makeMe.anAssessmentAttempt(notebook.getCreatorEntity()).withOneQuestion().please();
    assessmentQuestionInstance = assessmentAttempt.getAssessmentQuestionInstances().get(0);
  }

  @Test
  void testSendFeedbackReturnsOk() {
    String feedback = "This is a feedback";
    controller.sendFeedback(feedback, assessmentQuestionInstance);
    List<Conversation> conversations =
        (List<Conversation>) modelFactoryService.conversationRepository.findAll();
    assertEquals(1, conversations.size());
  }

  @Test
  void testGetMessageDetailWhenSendFeedbackReturnsOk() {
    String feedback = "This is a feedback";
    Conversation conversation = controller.sendFeedback(feedback, assessmentQuestionInstance);

    List<Conversation> conversations =
        (List<Conversation>) modelFactoryService.conversationRepository.findAll();

    var conversationDetail =
        modelFactoryService.conversationMessageRepository.findByConversationInitiator(
            conversation.getId());
    assertEquals(1, conversations.size());
    assertEquals(1, conversationDetail.size());
    assertEquals(feedback, conversationDetail.getFirst().getMessage());
  }

  @Test
  void testGetFeedbackThreadsSendFromTheUser() {
    makeMe
        .aConversation()
        .forAnAssessmentQuestionInstance(assessmentQuestionInstance)
        .from(currentUser)
        .please();
    makeMe.aConversation().forAnAssessmentQuestionInstance(assessmentQuestionInstance).please();
    List<Conversation> conversations = controller.getConversationsOfCurrentUser();
    assertEquals(1, conversations.size());
  }

  @Test
  void testGetFeedbackThreadsAsReceiver() {
    makeMe
        .theNotebook(assessmentQuestionInstance.getAssessmentAttempt().getNotebook())
        .owner(currentUser.getEntity())
        .please();
    makeMe.aConversation().forAnAssessmentQuestionInstance(assessmentQuestionInstance).please();
    List<Conversation> conversations = controller.getConversationsOfCurrentUser();
    assertEquals(1, conversations.size());
  }

  @Test
  void testGetFeedbackThreadsAsAMemberOfACircle() {
    Circle myCircle = makeMe.aCircle().hasMember(currentUser.getEntity()).please();
    makeMe
        .theNotebook(assessmentQuestionInstance.getAssessmentAttempt().getNotebook())
        .owner(myCircle)
        .please();
    makeMe.aConversation().forAnAssessmentQuestionInstance(assessmentQuestionInstance).please();
    List<Conversation> conversations = controller.getConversationsOfCurrentUser();
    assertEquals(1, conversations.size());
  }

  @Test
  void testGetOneUnreadConversationCountofCurrentUser() {
    Conversation conversation = makeMe.aConversation().from(currentUser).please();

    makeMe.aConversationMessage().forConversationInstance(conversation).please();
    makeMe.aConversationMessage().forConversationInstance(conversation).please();
    int conversations = controller.getUnreadConversationCountOfCurrentUser();
    assertEquals(1, conversations);
  }

  @Test
  void testGetTwoUnreadConversationCountofCurrentUser() {
    Conversation fristConversation = makeMe.aConversation().from(currentUser).please();
    makeMe.aConversationMessage().forConversationInstance(fristConversation).please();

    Conversation secondConversation = makeMe.aConversation().from(currentUser).please();
    makeMe.aConversationMessage().forConversationInstance(secondConversation).please();

    int conversations = controller.getUnreadConversationCountOfCurrentUser();
    assertEquals(2, conversations);
  }

  @Test
  void testZeroUnreadConversationCountForSender() {
    Conversation fristConversation = makeMe.aConversation().from(currentUser).please();
    ConversationMessage msg =
        makeMe.aConversationMessage().forConversationInstance(fristConversation).please();
    msg.setSender(currentUser.getEntity());

    int conversations = controller.getUnreadConversationCountOfCurrentUser();
    assertEquals(0, conversations);
  }

  @Test
  void testGetTOneUnreadAndOneReadConversationCountofCurrentUser() {
    Conversation fristConversation = makeMe.aConversation().from(currentUser).please();
    makeMe.aConversationMessage().forConversationInstance(fristConversation).please();

    Conversation secondConversation = makeMe.aConversation().from(currentUser).please();
    var msg = makeMe.aConversationMessage().forConversationInstance(secondConversation).please();
    msg.setIs_read(true);

    int conversations = controller.getUnreadConversationCountOfCurrentUser();
    assertEquals(1, conversations);
  }

  @Test
  void testMarkConversationAsReadByReceiver() throws UnexpectedNoAccessRightException {
    Conversation conversation = makeMe.aConversation().from(currentUser).please();
    ConversationMessage msg =
        makeMe.aConversationMessage().forConversationInstance(conversation).please();
    UserModel receiver = makeMe.aUser().toModelPlease();
    msg.setSender(receiver.getEntity());
    assertEquals(false, msg.getIs_read());

    controller.markConversationAsRead(conversation);
    assertEquals(true, msg.getIs_read());
  }

  @Test
  void testMarkConversationAsReadBySender() throws UnexpectedNoAccessRightException {
    Conversation conversation = makeMe.aConversation().from(currentUser).please();
    ConversationMessage msg =
        makeMe.aConversationMessage().forConversationInstance(conversation).please();
    msg.setSender(currentUser.getEntity());

    controller.markConversationAsRead(conversation);
    assertEquals(false, msg.getIs_read());
  }

  @Test
  void shouldNotBeAbleToReplyToAConversationIAmNotIn() {
    Conversation conversation = makeMe.aConversation().please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.replyToConversation("hi", conversation));
  }

  @Test
  void topicOwnerShouldBeAbleToReply() throws UnexpectedNoAccessRightException {
    String message = "This is a message";
    makeMe
        .theNotebook(assessmentQuestionInstance.getAssessmentAttempt().getNotebook())
        .owner(currentUser.getEntity())
        .please();
    Conversation conversation =
        makeMe.aConversation().forAnAssessmentQuestionInstance(assessmentQuestionInstance).please();
    ConversationMessage conversationMessage = controller.replyToConversation(message, conversation);
    List<ConversationMessage> conversationMessages =
        (List<ConversationMessage>) modelFactoryService.conversationMessageRepository.findAll();
    assertEquals(1, conversationMessages.size());
    assertEquals(message, conversationMessage.getMessage());
  }

  @Test
  void initiatorShouldBeAbleToReply() throws UnexpectedNoAccessRightException {
    String message = "This is a message";
    Conversation conversation = makeMe.aConversation().from(currentUser).please();
    ConversationMessage conversationMessage = controller.replyToConversation(message, conversation);
    assertEquals(message, conversationMessage.getMessage());
  }

  @Test
  void shouldNotBeAbleToSeeAConversationIAmNotIn() {
    Conversation conversation = makeMe.aConversation().please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.getConversationDetails(conversation));
  }

  @Test
  void testGetMessageThreadsFromConversation() throws UnexpectedNoAccessRightException {
    Conversation conversation = makeMe.aConversation().from(currentUser).please();

    makeMe.aConversationMessage().forConversationInstance(conversation).please();
    List<ConversationMessage> conversations = controller.getConversationDetails(conversation);
    assertEquals(1, conversations.size());
  }
}
