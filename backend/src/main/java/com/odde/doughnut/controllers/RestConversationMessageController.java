package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.AssessmentQuestionInstance;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationMessage;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
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
        conversationService.startConversation(assessmentQuestionInstance, currentUser.getEntity());
    conversationService.addMessageToConversation(conversation, currentUser.getEntity(), feedback);
    return conversation;
  }

  @GetMapping("/all")
  public List<Conversation> getConversationsOfCurrentUser() {
    currentUser.assertLoggedIn();
    return conversationService.conversationRelatedToUser(currentUser.getEntity());
  }

  @GetMapping("/unreadCount")
  public int getUnreadConversationCountOfCurrentUser() {
    currentUser.assertLoggedIn();
    List<ConversationMessage> conversationMessages =
        conversationService.getConversationMessages(currentUser.getEntity());
    return (int)
        conversationMessages.stream()
            .filter(message -> !Boolean.TRUE.equals(message.getIs_read()) && !message.getSender().equals(currentUser.getEntity()))
            .map(ConversationMessage::getConversation)
            .distinct()
            .count();
  }

  @PatchMapping("/read/{conversationId}")
  public void markConversationAsRead(
      @PathVariable("conversationId") @Schema(type = "integer") Conversation conversation)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(conversation);
    conversationService.markConversationAsRead(conversation, currentUser.getEntity());
  }

  @PostMapping("/detail/send/{conversationId}")
  public ConversationMessage replyToConversation(
      @RequestBody String message,
      @PathVariable("conversationId") @Schema(type = "integer") Conversation conversation)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(conversation);
    return conversationService.addMessageToConversation(
        conversation, currentUser.getEntity(), message);
  }

  @GetMapping("/detail/all/{conversationId}")
  public List<ConversationMessage> getConversationDetails(
      @PathVariable("conversationId") @Schema(type = "integer") Conversation conversation)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(conversation);
    return conversationService.getConversionDetailRelatedByConversationId(conversation.getId());
  }
}
