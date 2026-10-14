package com.odde.donut.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.controllers.dto.ConversationCorrectionAcceptance;
import com.odde.donut.controllers.dto.ConversationListItem;
import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.ConversationMessage;
import com.odde.donut.entities.ConversationNoteCorrection;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.exceptions.OpenAiNotAvailableException;
import com.odde.donut.exceptions.OpenAiUnauthorizedException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.ConversationCorrectionAcceptanceService;
import com.odde.donut.services.ConversationService;
import com.odde.donut.services.ai.NoteConversationAiReplyService;
import com.openai.models.responses.ResponseCreateParams;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.apache.coyote.BadRequestException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/conversation")
public class ConversationMessageController {
  private final ConversationService conversationService;
  private final NoteConversationAiReplyService noteConversationAiReplyService;
  private final AuthorizationService authorizationService;
  private final ObjectMapper objectMapper;
  private final ConversationCorrectionAcceptanceService correctionAcceptanceService;

  public ConversationMessageController(
      ConversationService conversationService,
      NoteConversationAiReplyService noteConversationAiReplyService,
      AuthorizationService authorizationService,
      ObjectMapper objectMapper,
      ConversationCorrectionAcceptanceService correctionAcceptanceService) {
    this.conversationService = conversationService;
    this.noteConversationAiReplyService = noteConversationAiReplyService;
    this.authorizationService = authorizationService;
    this.objectMapper = objectMapper;
    this.correctionAcceptanceService = correctionAcceptanceService;
  }

  @PostMapping("/note/{note}")
  @Transactional
  public Conversation startConversationAboutNote(
      @PathVariable("note") @Schema(type = "integer") Note note, @RequestBody String message) {
    return conversationService.startConversationOfNote(
        note, authorizationService.getCurrentUser(), message);
  }

  @GetMapping("/all")
  public List<ConversationListItem> getConversationsOfCurrentUser() {
    authorizationService.assertLoggedIn();
    return conversationService.conversationListForUser(authorizationService.getCurrentUser());
  }

  @PatchMapping("/{conversationId}/read")
  public List<ConversationMessage> markConversationAsRead(
      @PathVariable("conversationId") @Schema(type = "integer") Conversation conversation)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(conversation);
    conversationService.markConversationAsRead(conversation, authorizationService.getCurrentUser());
    return conversationService.getUnreadMessages(authorizationService.getCurrentUser());
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
      if (conversation.getSubjectNote() == null) {
        throw new RuntimeException(
            "Only note or recall prompt related conversation can have AI reply");
      }

      return noteConversationAiReplyService.getReplyStream(conversation, conversationService);
    } catch (OpenAiUnauthorizedException | OpenAiNotAvailableException e) {
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

  @GetMapping("/{conversationId}/corrections")
  public List<ConversationNoteCorrection> getConversationCorrections(
      @PathVariable("conversationId") @Schema(type = "integer") Conversation conversation)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(conversation);
    return conversationService.getCorrections(conversation);
  }

  @PostMapping("/corrections/{correctionId}/decline")
  @Transactional
  public ConversationNoteCorrection declineCorrection(@PathVariable Integer correctionId)
      throws UnexpectedNoAccessRightException {
    ConversationNoteCorrection correction =
        conversationService.getCorrectionForUpdate(correctionId);
    authorizationService.assertAuthorization(correction.getSourceMessage().getConversation());
    return conversationService.declineCorrection(correction, authorizationService.getCurrentUser());
  }

  @PostMapping("/corrections/{correctionId}/accept")
  public ConversationNoteCorrection acceptCorrection(
      @PathVariable Integer correctionId,
      @Valid @RequestBody ConversationCorrectionAcceptance review)
      throws UnexpectedNoAccessRightException {
    return correctionAcceptanceService.accept(correctionId, review);
  }

  @GetMapping(value = "/{conversationId}/export", produces = "application/json")
  @org.springframework.web.bind.annotation.ResponseBody
  public Map<String, Object> exportConversation(
      @PathVariable("conversationId") @Schema(type = "integer") Conversation conversation)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(conversation);
    ResponseCreateParams params =
        noteConversationAiReplyService.buildResponseCreateParams(conversation);
    return serializeResponseCreateParams(params);
  }

  private Map<String, Object> serializeResponseCreateParams(ResponseCreateParams params) {
    try {
      ResponseCreateParams.Body body = params._body();
      String jsonString = objectMapper.writeValueAsString(body);
      Map<String, Object> result =
          objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
      removeValidFields(result);
      return result;
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize ResponseCreateParams", e);
    }
  }

  @SuppressWarnings("unchecked")
  private void removeValidFields(Object obj) {
    if (obj == null) {
      return;
    }
    if (obj instanceof Map) {
      Map<String, Object> map = (Map<String, Object>) obj;
      map.remove("valid");
      for (Object value : map.values()) {
        removeValidFields(value);
      }
    } else if (obj instanceof List) {
      List<?> list = (List<?>) obj;
      for (Object item : list) {
        removeValidFields(item);
      }
    }
  }

  @GetMapping("/note/{note}")
  public List<Conversation> getConversationsAboutNote(
      @PathVariable("note") @Schema(type = "integer") Note note) {
    authorizationService.assertLoggedIn();
    return conversationService.getConversationsAboutNote(
        note, authorizationService.getCurrentUser());
  }

  @PostMapping("/recall-prompt/{recallPrompt}")
  public Conversation startConversationAboutRecallPrompt(
      @PathVariable("recallPrompt") @Schema(type = "integer") RecallPrompt recallPrompt)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(recallPrompt.requireMemoryTracker());
    return conversationService.startConversationAboutRecallPrompt(
        recallPrompt, authorizationService.getCurrentUser());
  }
}
