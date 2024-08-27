package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.FeedbackDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestFeedbackControllerTest {

  RestFeedbackController controller = new RestFeedbackController();

  @Test
  void testSendFeedbackReturnsOk() {
     FeedbackDTO feedbackDTO = new FeedbackDTO();
     feedbackDTO.setFeedback("This is a feedback");
     assertEquals("Feedback received successfully!", controller.sendFeedback(feedbackDTO).getBody());
  }

}
