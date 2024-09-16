package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.AssessmentQuestionInstance;
import com.odde.doughnut.entities.Conversation;
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

  @PostMapping("/send/{assessmentQuestion}")
  public ResponseEntity<String> sendFeedback(
      @RequestBody String feedback,
      @PathVariable("assessmentQuestion") @Schema(type = "integer")
          AssessmentQuestionInstance assessmentQuestionInstance) {
    conversationService.startConversation(
        assessmentQuestionInstance, currentUser.getEntity(), feedback);
    return ResponseEntity.ok("Feedback received successfully!");
  }

  @GetMapping("/all")
  public List<Conversation> getFeedbackThreadsForUser() {
    currentUser.assertLoggedIn();
    return modelFactoryService.conversationRepository
        .findByUserInSubjectOwnershipOrConversationInitiator(currentUser.getEntity());
  }
}
