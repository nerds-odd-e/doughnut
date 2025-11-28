package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.AssessmentQuestionInstance;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationMessage;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionAnswer;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.ConversationService;
import com.odde.doughnut.services.ai.ChatCompletionConversationService;
import com.odde.doughnut.services.ai.ChatMessageForFineTuning;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
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
  private final ChatCompletionConversationService chatCompletionConversationService;
  private final AuthorizationService authorizationService;

  public ConversationMessageController(
      ConversationService conversationService,
      ChatCompletionConversationService chatCompletionConversationService,
      AuthorizationService authorizationService) {
    this.conversationService = conversationService;
    this.chatCompletionConversationService = chatCompletionConversationService;
    this.authorizationService = authorizationService;
  }

  @PostMapping("/assessment-question/{assessmentQuestion}")
  public Conversation startConversationAboutAssessmentQuestion(
      @RequestBody String feedback,
      @PathVariable("assessmentQuestion") @Schema(type = "integer")
          AssessmentQuestionInstance assessmentQuestionInstance) {
    Conversation conversation =
        conversationService.startConversationAboutRecallPrompt(
            assessmentQuestionInstance, authorizationService.getCurrentUser());
    conversationService.addMessageToConversation(
        conversation, authorizationService.getCurrentUser(), feedback);
    return conversation;
  }

  @PostMapping("/note/{note}")
  @Transactional
  public Conversation startConversationAboutNote(
      @PathVariable("note") @Schema(type = "integer") Note note, @RequestBody String message) {
    return conversationService.startConversationOfNote(
        note, authorizationService.getCurrentUser(), message);
  }

  @GetMapping("/all")
  public List<Conversation> getConversationsOfCurrentUser() {
    authorizationService.assertLoggedIn();
    return conversationService.conversationRelatedToUser(authorizationService.getCurrentUser());
  }

  @GetMapping("/unread")
  public List<ConversationMessage> getUnreadConversations() {
    authorizationService.assertLoggedIn();
    return conversationService.getUnreadConversations(authorizationService.getCurrentUser());
  }

  @PatchMapping("/{conversationId}/read")
  public List<ConversationMessage> markConversationAsRead(
      @PathVariable("conversationId") @Schema(type = "integer") Conversation conversation)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(conversation);
    conversationService.markConversationAsRead(conversation, authorizationService.getCurrentUser());
    return conversationService.getUnreadConversations(authorizationService.getCurrentUser());
  }

  @PostMapping("/{conversationId}/send")
  @Transactional
  public ConversationMessage replyToConversation(
      @RequestBody String message,
      @PathVariable("conversationId") @Schema(type = "integer") Conversation conversation)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(conversation);
    return conversationService.addMessageToConversation(
        conversation, authorizationService.getCurrentUser(), message);
  }

  @GetMapping("/{conversationId}")
  public Conversation getConversation(
      @PathVariable("conversationId") @Schema(type = "integer") Conversation conversation)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(conversation);
    return conversation;
  }

  @PostMapping("/{conversationId}/ai-reply")
  @Transactional
  public SseEmitter getAiReply(
      @PathVariable("conversationId") @Schema(type = "integer") Conversation conversation)
      throws UnexpectedNoAccessRightException, BadRequestException {
    authorizationService.assertAuthorization(conversation);
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
    authorizationService.assertAuthorization(conversation);
    return conversation.getConversationMessages();
  }

  @GetMapping(value = "/{conversationId}/export", produces = "application/json")
  @org.springframework.web.bind.annotation.ResponseBody
  public java.util.Map<String, Object> exportConversation(
      @PathVariable("conversationId") @Schema(type = "integer") Conversation conversation)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(conversation);
    ChatCompletionCreateParams params =
        chatCompletionConversationService.buildChatCompletionRequest(conversation);
    // Manually construct Map for proper JSON serialization
    java.util.Map<String, Object> result = new java.util.HashMap<>();
    result.put("model", params.model().toString());
    java.util.List<java.util.Map<String, Object>> messagesList = new java.util.ArrayList<>();
    for (ChatCompletionMessageParam messageParam : params.messages()) {
      java.util.Map<String, Object> messageMap = new java.util.HashMap<>();
      if (messageParam.system().isPresent()) {
        messageMap.put("role", "system");
        messageMap.put(
            "content",
            ChatMessageForFineTuning.extractContentString(messageParam.system().get().content()));
      } else if (messageParam.user().isPresent()) {
        messageMap.put("role", "user");
        messageMap.put(
            "content",
            ChatMessageForFineTuning.extractContentString(messageParam.user().get().content()));
      } else if (messageParam.assistant().isPresent()) {
        messageMap.put("role", "assistant");
        messageMap.put(
            "content",
            messageParam
                .assistant()
                .get()
                .content()
                .map(ChatMessageForFineTuning::extractContentString)
                .orElse(null));
      }
      messagesList.add(messageMap);
    }
    result.put("messages", messagesList);
    return result;
  }

  @GetMapping("/note/{note}")
  public List<Conversation> getConversationsAboutNote(
      @PathVariable("note") @Schema(type = "integer") Note note) {
    authorizationService.assertLoggedIn();
    return conversationService.getConversationsAboutNote(
        note, authorizationService.getCurrentUser());
  }

  @PostMapping("/question-answer/{questionAnswer}")
  public Conversation startConversationAboutQuestionAnswer(
      @PathVariable("questionAnswer") @Schema(type = "integer") QuestionAnswer questionAnswer) {
    return conversationService.startConversationAboutQuestionAnswer(
        questionAnswer, authorizationService.getCurrentUser());
  }
}
