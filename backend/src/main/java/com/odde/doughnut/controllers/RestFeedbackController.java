package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.AssessmentQuestionInstance;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationDetail;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ConversationDetailService;
import com.odde.doughnut.services.ConversationService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
public class RestFeedbackController {
  private final ConversationService conversationService;
  private final ConversationDetailService conversationDetailService;
  private final UserModel currentUser;

  public RestFeedbackController(
      UserModel currentUser,
      ConversationService conversationService,
      ConversationDetailService conversationDetailService) {
    this.currentUser = currentUser;
    this.conversationService = conversationService;
    this.conversationDetailService = conversationDetailService;
  }

  @PostMapping("/send/{assessmentQuestion}")
  public Conversation sendFeedback(
      @RequestBody String feedback,
      @PathVariable("assessmentQuestion") @Schema(type = "integer")
          AssessmentQuestionInstance assessmentQuestionInstance) {
    Conversation conversation =
        conversationService.startConversation(
            assessmentQuestionInstance, currentUser.getEntity(), feedback);
    conversationDetailService.addConversationDetail(
        conversation, currentUser.getEntity(), feedback);
    return conversation;
  }

  @GetMapping("/all")
  public List<Conversation> getFeedbackThreadsForUser() {
    currentUser.assertLoggedIn();
    return conversationService.conversationRelatedToUser(currentUser.getEntity());
  }

  @PostMapping("/detail/send/{conversationId}")
  public ConversationDetail sendMessage(
      @RequestBody String message,
      @PathVariable("conversationId") @Schema(type = "integer") Conversation conversation) {
    return conversationDetailService.addConversationDetail(
        conversation, currentUser.getEntity(), message);
  }

  @GetMapping("/detail/all/{conversationId}")
  public List<ConversationDetail> getMessageThreadsForConversation(
      @PathVariable("conversationId") @Schema(type = "integer") Conversation conversation) {
    currentUser.assertLoggedIn();
    return conversationDetailService.getConversionDetailRelatedByConversationId(
        conversation.getId());
  }
}
