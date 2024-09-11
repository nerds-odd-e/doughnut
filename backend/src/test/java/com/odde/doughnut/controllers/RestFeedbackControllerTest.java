package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.entities.User;
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
    PredefinedQuestion predefinedQuestion = makeMe.aQuestion().please();

    ResponseEntity<String> response =
        controller.sendFeedback(feedback, predefinedQuestion.getQuizQuestion());

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
    User feedbackGiverUser = makeMe.aUser().please();

    PredefinedQuestion predefinedQuestion1 = makeMe.aQuestion().please();
    predefinedQuestion1.getNote().setCreator(this.currentUser.getEntity());
    Conversation conversation1 = new Conversation();
    conversation1.setConversationInitiator(feedbackGiverUser);
    conversation1.setNoteCreator(predefinedQuestion1.getNote().getCreator());
    conversation1.setMessage("This is a feedback for the current user");
    conversation1.setQuizQuestion(predefinedQuestion1.getQuizQuestion());
    makeMe.modelFactoryService.save(conversation1);

    PredefinedQuestion predefinedQuestion2 = makeMe.aQuestion().please();
    Conversation conversation2 = new Conversation();
    conversation2.setConversationInitiator(feedbackGiverUser);
    conversation2.setNoteCreator(predefinedQuestion2.getNote().getCreator());
    conversation2.setMessage("This is a feedback for the other user");
    conversation2.setQuizQuestion(predefinedQuestion2.getQuizQuestion());
    makeMe.modelFactoryService.save(conversation2);

    List<Conversation> conversations = controller.getFeedback();

    assertEquals(1, conversations.size());
    assertEquals("This is a feedback for the current user", conversations.getFirst().getMessage());
  }

  @Test
  void testGetFeedbackThreadsForUser() {
    User notCurrentUser = makeMe.aUser().please();

    PredefinedQuestion predefinedQuestion1 = makeMe.aQuestion().please();
    predefinedQuestion1.getNote().setCreator(this.currentUser.getEntity());
    Conversation conversation1 = new Conversation();
    conversation1.setConversationInitiator(notCurrentUser);
    conversation1.setNoteCreator(predefinedQuestion1.getNote().getCreator());
    conversation1.setMessage("This is a feedback for the current user");
    conversation1.setQuizQuestion(predefinedQuestion1.getQuizQuestion());
    makeMe.modelFactoryService.save(conversation1);

    PredefinedQuestion predefinedQuestion2 = makeMe.aQuestion().please();
    predefinedQuestion2.getNote().setCreator(notCurrentUser);
    Conversation conversation2 = new Conversation();
    conversation2.setConversationInitiator(this.currentUser.getEntity());
    conversation2.setNoteCreator(predefinedQuestion2.getNote().getCreator());
    conversation2.setMessage("This is a feedback for the current user");
    conversation2.setQuizQuestion(predefinedQuestion2.getQuizQuestion());
    makeMe.modelFactoryService.save(conversation2);

    User notCurrentUser2 = makeMe.aUser().please();

    PredefinedQuestion predefinedQuestion3 = makeMe.aQuestion().please();
    predefinedQuestion3.getNote().setCreator(notCurrentUser);
    Conversation conversation3 = new Conversation();
    conversation3.setConversationInitiator(notCurrentUser2);
    conversation3.setNoteCreator(predefinedQuestion3.getNote().getCreator());
    conversation3.setMessage("This is a feedback for the current user");
    conversation3.setQuizQuestion(predefinedQuestion3.getQuizQuestion());
    makeMe.modelFactoryService.save(conversation3);

    List<Conversation> conversations = controller.getFeedbackThreadsForUser();

    assertEquals(2, conversations.size());
  }
}
