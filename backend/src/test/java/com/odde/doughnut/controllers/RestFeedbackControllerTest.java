package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.controllers.dto.FeedbackDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionAndAnswer;
import com.odde.doughnut.services.ConversationService;
import com.odde.doughnut.testability.MakeMe;
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
}
