package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ReviewQuestionInstance;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ConversationService;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestFeedbackControllerTest {

  @Autowired ConversationService conversationService;
  @Autowired MakeMe makeMe;
  private UserModel currentUser;
  RestFeedbackController controller;

  @Autowired ModelFactoryService modelFactoryService;

  @BeforeEach
  void setup() {
    currentUser = makeMe.aUser().toModelPlease();
    controller = new RestFeedbackController(currentUser, conversationService, modelFactoryService);
  }

  @Test
  void testSendFeedbackReturnsOk() {
    String feedback = "This is a feedback";
    ReviewQuestionInstance reviewQuestionInstance = makeMe.aQuizQuestion().please();

    ResponseEntity<String> response = controller.sendFeedback(feedback, reviewQuestionInstance);

    List<Conversation> conversations =
        (List<Conversation>) modelFactoryService.conversationRepository.findAll();
    assertEquals("Feedback received successfully!", response.getBody());
    assertEquals(1, conversations.size());
    assertEquals(feedback, conversations.getFirst().getMessage());
  }

  @Test
  void testGetFeedbackReturnsZeroConversationsForCurrentUser() {
    List<Conversation> conversations = controller.getFeedback();
    assertEquals(0, conversations.size());
  }

  @Test
  void testGetFeedbackReturnsAllConversationsForCurrentUser() {
    makeMe
        .aConversation()
        .forAQuizQuestion()
        .to(currentUser)
        .messagge("This is a feedback for the current user")
        .please();
    List<Conversation> conversations = controller.getFeedback();
    assertEquals(1, conversations.size());
    assertEquals("This is a feedback for the current user", conversations.getFirst().getMessage());
  }

  @Test
  void testGetFeedbackThreadsForUser() {
    makeMe.aConversation().forAQuizQuestion().to(currentUser).please();
    makeMe.aConversation().forAQuizQuestion().from(currentUser).please();
    makeMe.aConversation().forAQuizQuestion().please();
    List<Conversation> conversations = controller.getFeedbackThreadsForUser();

    assertEquals(2, conversations.size());
  }
}
