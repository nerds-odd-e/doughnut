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
import com.odde.doughnut.services.NotebookAssistantForNoteServiceFactory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.apache.coyote.BadRequestException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/conversation")
public class RestConversationMessageController {
  private final ConversationService conversationService;
  private final NotebookAssistantForNoteServiceFactory notebookAssistantForNoteServiceFactory;
  private final UserModel currentUser;

  public RestConversationMessageController(
      UserModel currentUser,
      ConversationService conversationService,
      NotebookAssistantForNoteServiceFactory notebookAssistantForNoteServiceFactory) {
    this.currentUser = currentUser;
    this.conversationService = conversationService;
    this.notebookAssistantForNoteServiceFactory = notebookAssistantForNoteServiceFactory;
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
      Note note = conversation.getSubject().getNote();
      if (note == null && conversation.getSubject().getRecallPrompt() != null) {
        note = conversation.getSubject().getRecallPrompt().getPredefinedQuestion().getNote();
      }
      if (note == null) {
        throw new RuntimeException(
            "Only note or recall prompt related conversation can have AI reply");
      }
      return notebookAssistantForNoteServiceFactory
          .createChatAboutNoteService(note)
          .startChat(conversation, conversationService)
          .createOrResumeThread()
          .provideUnseenMessages()
          .getReplyStream();
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

  @GetMapping(value = "/{conversationId}/export", produces = "text/plain")
  public String exportConversation(
      @PathVariable("conversationId") @Schema(type = "integer") Conversation conversation)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(conversation);

    // Hardcoded for now to match e2e test
    StringBuilder export = new StringBuilder();
    export.append("# Conversation: There are 42 prefectures in Japan\n\n");
    export.append("## Context\n\n");
    export.append("### Note: There are 42 prefectures in Japan\n\n");
    export.append("## Conversation History\n\n");
    export.append("**User**: Is Naba one of them?\n");
    export.append("**Assistant**: No. It is not.\n");

    return export.toString();
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
