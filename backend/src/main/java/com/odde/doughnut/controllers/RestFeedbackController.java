package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.QuizQuestionAndAnswer;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ConversationService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
public class RestFeedbackController {
  private final ConversationService conversationService;
  private final ModelFactoryService modelFactoryService;
  private final UserModel currentUser;

  public RestFeedbackController(
      UserModel currentUser,
      ConversationService conversationService,
      ModelFactoryService modelFactoryService) {
    this.currentUser = currentUser;
    this.conversationService = conversationService;
    this.modelFactoryService = modelFactoryService;
  }

  @PostMapping("/send/{question}")
  public ResponseEntity<String> sendFeedback(
      @RequestBody String feedback,
      @PathVariable("question") @Schema(type = "integer")
          QuizQuestionAndAnswer quizQuestionAndAnswer) {
    conversationService.startConversation(quizQuestionAndAnswer, currentUser.getEntity(), feedback);
    return ResponseEntity.ok("Feedback received successfully!");
  }

  @GetMapping
  public List<Conversation> getFeedback() {
    currentUser.assertLoggedIn();
    return modelFactoryService.conversationRepository.findByNoteCreator(currentUser.getEntity());
  }
}
