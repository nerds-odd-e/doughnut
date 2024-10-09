package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ConversationDetailService;
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
class RestFeedbackControllerTest {

  @Autowired ConversationService conversationService;
  @Autowired ConversationDetailService conversationDetailService;
  @Autowired MakeMe makeMe;
  private UserModel currentUser;
  RestFeedbackController controller;

  @Autowired ModelFactoryService modelFactoryService;
  AssessmentQuestionInstance assessmentQuestionInstance;

  @BeforeEach
  void setup() {
    currentUser = makeMe.aUser().toModelPlease();
    controller =
        new RestFeedbackController(currentUser, conversationService, conversationDetailService);
    Notebook notebook = makeMe.aNotebook().please();
    AssessmentAttempt assessmentAttempt =
        makeMe.anAssessmentAttempt(notebook.getCreatorEntity()).withOneQuestion().please();
    assessmentQuestionInstance = assessmentAttempt.getAssessmentQuestionInstances().get(0);
  }

  @Test
  void testSendFeedbackReturnsOk() {
    String feedback = "This is a feedback";

    Conversation conversation = controller.sendFeedback(feedback, assessmentQuestionInstance);

    List<Conversation> conversations =
        (List<Conversation>) modelFactoryService.conversationRepository.findAll();
    assertEquals(1, conversations.size());
    assertEquals(feedback, conversation.getMessage());
  }

  @Test
  void testGetFeedbackThreadsSendFromTheUser() {
    makeMe
        .aConversation()
        .forAnAssessmentQuestionInstance(assessmentQuestionInstance)
        .from(currentUser)
        .please();
    makeMe.aConversation().forAnAssessmentQuestionInstance(assessmentQuestionInstance).please();
    List<Conversation> conversations = controller.getFeedbackThreadsForUser();
    assertEquals(1, conversations.size());
  }

  @Test
  void testGetFeedbackThreadsAsReceiver() {
    makeMe
        .theNotebook(assessmentQuestionInstance.getAssessmentAttempt().getNotebook())
        .owner(currentUser.getEntity())
        .please();
    makeMe.aConversation().forAnAssessmentQuestionInstance(assessmentQuestionInstance).please();
    List<Conversation> conversations = controller.getFeedbackThreadsForUser();
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
    List<Conversation> conversations = controller.getFeedbackThreadsForUser();
    assertEquals(1, conversations.size());
  }

  @Test
  void testSendMessageReturnsOk() {
    String message = "This is a message";
    makeMe
        .theNotebook(assessmentQuestionInstance.getAssessmentAttempt().getNotebook())
        .owner(currentUser.getEntity())
        .please();
    Conversation conversation =
        makeMe.aConversation().forAnAssessmentQuestionInstance(assessmentQuestionInstance).please();
    ConversationDetail conversationDetail = controller.sendMessage(message, conversation);
    List<ConversationDetail> conversationDetails =
        (List<ConversationDetail>) modelFactoryService.conversationDetailRepository.findAll();
    assertEquals(1, conversationDetails.size());
    assertEquals(message, conversationDetail.getMessage());
  }
}
