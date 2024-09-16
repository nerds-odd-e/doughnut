package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.AssessmentQuestionInstance;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ConversationService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
public class RestFeedbackController {
  private final ConversationService conversationService;
  private final UserModel currentUser;

  public RestFeedbackController(UserModel currentUser, ConversationService conversationService) {
    this.currentUser = currentUser;
    this.conversationService = conversationService;
  }

  @PostMapping("/send/{assessmentQuestion}")
  public Conversation sendFeedback(
      @RequestBody String feedback,
      @PathVariable("assessmentQuestion") @Schema(type = "integer")
          AssessmentQuestionInstance assessmentQuestionInstance) {
    return conversationService.startConversation(
        assessmentQuestionInstance, currentUser.getEntity(), feedback);
  }

  @GetMapping("/all")
  public List<Conversation> getFeedbackThreadsForUser() {
    currentUser.assertLoggedIn();
    return conversationService.conversationRelatedToUser(currentUser.getEntity());
  }
}
