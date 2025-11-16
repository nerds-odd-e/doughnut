package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.AssessmentQuestionInstance;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationMessage;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ConversationService;
import com.odde.doughnut.services.ai.ChatCompletionConversationService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.apache.coyote.BadRequestException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/conversation")
public class ConversationMessageController {
  private final ConversationService conversationService;
  private final UserModel currentUser;
  private final ChatCompletionConversationService chatCompletionConversationService;

  public ConversationMessageController(
      UserModel currentUser,
      ConversationService conversationService,
      ChatCompletionConversationService chatCompletionConversationService) {
    this.currentUser = currentUser;
    this.conversationService = conversationService;
    this.chatCompletionConversationService = chatCompletionConversationService;
  }

  @PostMapping("/assessment-question/{assessmentQuestion}")
  public Conversation startConversationAboutAssessmentQuestion(
      @RequestBody String feedback,
      @PathVariable("assessmentQuestion") @Schema(type = "integer")
          AssessmentQuestionInstance assessmentQuestionInstance) {
    Conversation conversation =
        conversationService.startConversationAboutRecallPrompt(
            assessmentQuestionInstance, currentUser.getEntity());
    conversationService.addMessageToConversation(conversation, currentUser.getEntity(), feedback);
    return conversation;
  }

  @PostMapping("/note/{note}")
  @Transactional
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
  @Transactional
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
      throws UnexpectedNoAccessRightException, BadRequestException {
    currentUser.assertAuthorization(conversation);
    try {
      Note note = conversation.getSubjectNote();
      if (note == null) {
        throw new RuntimeException(
            "Only note or recall prompt related conversation can have AI reply");
      }

      // Use new chat completion service
      return chatCompletionConversationService.getReplyStream(conversation, conversationService);
    } catch (OpenAiUnauthorizedException e) {
      // Since this method is asynchronous, the exception body is not returned to the client.
      // Instead, the client will receive a 400 Bad Request status code, with no body.
      throw new BadRequestException(e.getMessage(), e);
    }
  }

  @GetMapping("/{conversationId}/messages")
  public List<ConversationMessage> getConversationMessages(
      @PathVariable("conversationId") @Schema(type = "integer") Conversation conversation)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(conversation);
    return conversation.getConversationMessages();
  }

  @GetMapping(value = "/{conversationId}/export", produces = "application/json")
  public ChatCompletionRequest exportConversation(
      @PathVariable("conversationId") @Schema(type = "integer") Conversation conversation)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(conversation);
    return chatCompletionConversationService.buildChatCompletionRequest(conversation);
  }

  @GetMapping("/note/{note}")
  public List<Conversation> getConversationsAboutNote(
      @PathVariable("note") @Schema(type = "integer") Note note) {
    currentUser.assertLoggedIn();
    return conversationService.getConversationsAboutNote(note, currentUser.getEntity());
  }

  @PostMapping("/recall-prompt/{recallPrompt}")
  public Conversation startConversationAboutRecallPrompt(
      @PathVariable("recallPrompt") @Schema(type = "integer") RecallPrompt recallPrompt) {
    return conversationService.startConversationAboutRecallPrompt(
        recallPrompt, currentUser.getEntity());
  }
}
