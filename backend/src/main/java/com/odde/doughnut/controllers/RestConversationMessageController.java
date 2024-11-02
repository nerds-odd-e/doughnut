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
import com.odde.doughnut.services.ai.AssistantService;
import com.theokanning.openai.assistants.message.Message;
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
      throws UnexpectedNoAccessRightException {
    validateConversation(conversation);
    ChatAboutNoteService chatService = setupChatService(conversation);
    setupMessageHandler(conversation, chatService);
    return chatService.getAIReplySSE();
  }

  private void validateConversation(Conversation conversation)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(conversation);
    Note note = conversation.getSubject().getNote();
    if (note == null) {
      throw new RuntimeException("Only note related conversation can have AI reply");
    }
  }

  private ChatAboutNoteService setupChatService(Conversation conversation) {
    Note note = conversation.getSubject().getNote();
    String threadId = conversation.getAiAssistantThreadId();
    AssistantService assistantService = aiAdvisorWithStorageService.getChatAssistantService(note);
    if (threadId == null) {
      threadId =
          aiAdvisorWithStorageService.createThread(note.getCreator(), assistantService, note);
      conversationService.setConversationAiAssistantThreadId(conversation, threadId);
    }
    ChatAboutNoteService chatService =
        aiAdvisorWithStorageService.getChatAboutNoteService(threadId, assistantService);

    // Get unsynchronized messages and create one combined message
    List<ConversationMessage> unsynced =
        conversation.getConversationMessages().stream()
            .filter(
                msg ->
                    conversation.getLastAiAssistantThreadSync() == null
                        || msg.getCreatedAt().after(conversation.getLastAiAssistantThreadSync()))
            .filter(msg -> msg.getSender() != null) // Only user messages
            .toList();

    if (!unsynced.isEmpty()) {
      StringBuilder combinedMessage = new StringBuilder();
      for (ConversationMessage msg : unsynced) {
        combinedMessage.append(String.format("user `%s` says:%n", msg.getSender().getName()));
        combinedMessage.append("-----------------\n");
        combinedMessage.append(msg.getMessage());
        combinedMessage.append("\n\n");
      }
      chatService.createUserMessage(combinedMessage.toString());
    } else {
      chatService.createUserMessage("just say something.");
    }

    return chatService;
  }

  private void setupMessageHandler(Conversation conversation, ChatAboutNoteService chatService) {
    chatService.onMessageCompleted(
        message -> {
          String content = extractMessageContent(message);
          conversationService.addMessageToConversation(
              conversation,
              null, // AI message has no user
              content);
        });
  }

  private static String extractMessageContent(Message message) {
    return message.getContent().stream()
        .filter(c -> "text".equals(c.getType()))
        .map(c -> c.getText().getValue())
        .findFirst()
        .orElse("");
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
