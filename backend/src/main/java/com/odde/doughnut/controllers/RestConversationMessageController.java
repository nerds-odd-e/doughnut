package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.AssessmentQuestionInstance;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationMessage;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AiAdvisorWithStorageService;
import com.odde.doughnut.services.ChatAboutNoteService;
import com.odde.doughnut.services.ConversationService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/conversation")
public class RestConversationMessageController {
  private final ConversationService conversationService;
  private final AiAdvisorWithStorageService aiAdvisorWithStorageService;
  private final UserModel currentUser;

  public RestConversationMessageController(
      UserModel currentUser,
      ConversationService conversationService,
      AiAdvisorWithStorageService aiAdvisorWithStorageService) {
    this.currentUser = currentUser;
    this.conversationService = conversationService;
    this.aiAdvisorWithStorageService = aiAdvisorWithStorageService;
  }

  @PostMapping("/assessment-question/{assessmentQuestion}")
  public Conversation startConversationAboutAssessmentQuestion(
      @RequestBody String feedback,
      @PathVariable("assessmentQuestion") @Schema(type = "integer")
          AssessmentQuestionInstance assessmentQuestionInstance) {
    Conversation conversation =
        conversationService.startConversation(assessmentQuestionInstance, currentUser.getEntity());
    conversationService.addMessageToConversation(conversation, currentUser.getEntity(), feedback);
    return conversation;
  }

  @PostMapping("/note/{note}")
  public Conversation startConversationAboutNote(
      @PathVariable("note") @Schema(type = "integer") Note note, @RequestBody String message) {
    return conversationService.startConversationOfNote(note, currentUser.getEntity(), message);
  }

  @GetMapping("/all")
  public List<Conversation> getConversationsOfCurrentUser() {
    currentUser.assertLoggedIn();
    return conversationService.conversationRelatedToUser(currentUser.getEntity());
  }

  @GetMapping("/unread")
  public List<ConversationMessage> getUnreadConversations() {
    currentUser.assertLoggedIn();
    return conversationService.getUnreadConversations(currentUser.getEntity());
  }

  @PatchMapping("/{conversationId}/read")
  public List<ConversationMessage> markConversationAsRead(
      @PathVariable("conversationId") @Schema(type = "integer") Conversation conversation)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(conversation);
    conversationService.markConversationAsRead(conversation, currentUser.getEntity());
    return conversationService.getUnreadConversations(currentUser.getEntity());
  }

  @PostMapping("/{conversationId}/send")
  public ConversationMessage replyToConversation(
      @RequestBody String message,
      @PathVariable("conversationId") @Schema(type = "integer") Conversation conversation)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(conversation);
    return conversationService.addMessageToConversation(
        conversation, currentUser.getEntity(), message);
  }

  @GetMapping("/{conversationId}")
  public Conversation getConversation(
      @PathVariable("conversationId") @Schema(type = "integer") Conversation conversation)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(conversation);
    return conversation;
  }

  @PostMapping("/{conversationId}/ai-reply")
  @Transactional
  public SseEmitter getAiReply(
      @PathVariable("conversationId") @Schema(type = "integer") Conversation conversation)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(conversation);
    Note note = conversation.getSubject().getNote();
    if (note == null) {
      throw new RuntimeException("Only note related conversation can have AI reply");
    }
    ChatAboutNoteService chatService =
        aiAdvisorWithStorageService.getChatService(note, conversation.getAiAssistantThreadId());
    chatService.createUserMessage("just say something.");

    // Add event listener before getting the SSE emitter
    chatService.onMessageCompleted(
        message -> {
          String content =
              message.getContent().stream()
                  .filter(c -> "text".equals(c.getType()))
                  .map(c -> c.getText().getValue())
                  .findFirst()
                  .orElse("");

          conversationService.addMessageToConversation(
              conversation,
              null, // AI message has no user
              content);
        });

    return chatService.getAIReplySSE();
  }

  @GetMapping("/{conversationId}/messages")
  public List<ConversationMessage> getConversationMessages(
      @PathVariable("conversationId") @Schema(type = "integer") Conversation conversation)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(conversation);
    return conversation.getConversationMessages();
  }

  @GetMapping("/note/{note}")
  public List<Conversation> getConversationsAboutNote(
      @PathVariable("note") @Schema(type = "integer") Note note) {
    currentUser.assertLoggedIn();
    return conversationService.getConversationsAboutNote(note, currentUser.getEntity());
  }
}
