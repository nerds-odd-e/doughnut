package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.AssessmentQuestionInstance;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationDetail;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ConversationService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/message")
public class RestConversationMessageController {
  private final ConversationService conversationService;
  private final UserModel currentUser;

  public RestConversationMessageController(
      UserModel currentUser, ConversationService conversationService) {
    this.currentUser = currentUser;
    this.conversationService = conversationService;
  }

  @PostMapping("/send/{assessmentQuestion}")
  public Conversation sendFeedback(
      @RequestBody String feedback,
      @PathVariable("assessmentQuestion") @Schema(type = "integer")
          AssessmentQuestionInstance assessmentQuestionInstance) {
    Conversation conversation =
        conversationService.startConversation(
            assessmentQuestionInstance, currentUser.getEntity(), feedback);
    conversationService.addConversationDetail(conversation, currentUser.getEntity(), feedback);
    return conversation;
  }

  @GetMapping("/all")
  public List<Conversation> getConversationsOfCurrentUser() {
    currentUser.assertLoggedIn();
    return conversationService.conversationRelatedToUser(currentUser.getEntity());
  }

  @PostMapping("/detail/send/{conversationId}")
  public ConversationDetail replyToConversation(
      @RequestBody String message,
      @PathVariable("conversationId") @Schema(type = "integer") Conversation conversation) {
    return conversationService.addConversationDetail(
        conversation, currentUser.getEntity(), message);
  }

  @GetMapping("/detail/all/{conversationId}")
  public List<ConversationDetail> getConversationDetails(
      @PathVariable("conversationId") @Schema(type = "integer") Conversation conversation) {
    currentUser.assertLoggedIn();
    return conversationService.getConversionDetailRelatedByConversationId(conversation.getId());
  }
}
