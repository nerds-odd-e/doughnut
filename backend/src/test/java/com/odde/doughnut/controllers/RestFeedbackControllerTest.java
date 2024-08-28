package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.controllers.dto.FeedbackDTO;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionAndAnswer;
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

  @Autowired ConversationService feedbackService;

  @Autowired MakeMe makeMe;

  RestFeedbackController controller;

  @BeforeEach
  void setup() {
    controller = new RestFeedbackController(feedbackService);
  }

  @Test
  void testSendFeedbackReturnsOk() {
    FeedbackDTO feedbackDTO = new FeedbackDTO();
    feedbackDTO.setFeedback("This is a feedback");

    Note note = makeMe.aNote().creatorAndOwner(makeMe.aUser().please()).please();
    QuizQuestionAndAnswer quizQuestionAndAnswer = new QuizQuestionAndAnswer();
    quizQuestionAndAnswer.setNote(note);

    assertEquals(
        "Feedback received successfully!",
        controller.sendFeedback(feedbackDTO, quizQuestionAndAnswer).getBody());
  }

  @Test
  void testGetFeedbackReturnsZeroConversationsForCurrentUser() {
    List<Conversation> conversations = controller.getFeedback();
    assertEquals(0, conversations.size());
  }

  //  @Test
  //  void testGetFeedbackReturnsAllConversationsForCurrentUser() {
  //    // Given 10 fake conversations in the database for the test user
  //    Conversation conversation = makeMe.aConversation().please();
  //
  //    // When I call getFeedback, get all conversations for the current user
  //    List<Conversation> conversations = controller.getFeedback();
  //
  //    // Then I should get all 10 conversations for the test user
  //    assertEquals(10, conversations.size());
  //  }
}
