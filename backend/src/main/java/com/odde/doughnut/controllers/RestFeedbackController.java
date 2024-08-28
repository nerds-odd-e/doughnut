package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.FeedbackDTO;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.QuizQuestionAndAnswer;
import com.odde.doughnut.services.ConversationService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
public class RestFeedbackController {
  private final ConversationService conversationService;

  public RestFeedbackController(ConversationService conversationService) {
    this.conversationService = conversationService;
  }

  @PostMapping("/send/{question}")
  public ResponseEntity<String> sendFeedback(
      @RequestBody FeedbackDTO feedbackDTO,
      @PathVariable("question") @Schema(type = "integer")
          QuizQuestionAndAnswer quizQuestionAndAnswer) {
    conversationService.startConversation(quizQuestionAndAnswer, feedbackDTO);
    return ResponseEntity.ok("Feedback received successfully!");
  }

  @GetMapping
  public List<Conversation> getFeedback() {
    return new ArrayList<>();
  }
}
