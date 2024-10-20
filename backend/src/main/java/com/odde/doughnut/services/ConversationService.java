package com.odde.doughnut.services;

import com.odde.doughnut.entities.AssessmentQuestionInstance;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationMessage;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ConversationService {

  private final ModelFactoryService modelFactoryService;

  public Conversation startConversation(
      AssessmentQuestionInstance assessmentQuestionInstance, User initiator) {
    Conversation conversation = new Conversation();
    conversation.setAssessmentQuestionInstance(assessmentQuestionInstance);
    conversation.setConversationInitiator(initiator);
    modelFactoryService.conversationRepository.save(conversation);
    return conversation;
  }

  public Conversation startConversationOfNote(Note note, User initiator) {
    Conversation conversation = new Conversation();
    conversation.setNote(note);
    conversation.setConversationInitiator(initiator);
    modelFactoryService.conversationRepository.save(conversation);
    return conversation;
  }

  public List<Conversation> conversationRelatedToUser(User user) {
    return modelFactoryService.conversationRepository
        .findByUserInSubjectOwnershipOrConversationInitiator(user);
  }

  public ConversationMessage addMessageToConversation(
      Conversation conversation, User user, String message) {
    ConversationMessage conversationMessage = new ConversationMessage();
    conversationMessage.setConversation(conversation);
    conversationMessage.setSender(user);
    conversationMessage.setMessage(message);
    conversationMessage.setIs_read(false);
    return modelFactoryService.conversationMessageRepository.save(conversationMessage);
  }

  public List<ConversationMessage> getConversionDetailRelatedByConversationId(int conversationId) {
    return modelFactoryService.conversationMessageRepository.findByConversationInitiator(
        conversationId);
  }

  public List<ConversationMessage> getConversationDetailsByConversationIds(
      List<Integer> conversationIds) {
    return conversationIds.stream()
        .flatMap(id -> getConversionDetailRelatedByConversationId(id).stream())
        .collect(Collectors.toList());
  }

  public List<ConversationMessage> getConversationMessages(User user) {
    List<Conversation> conversations = conversationRelatedToUser(user);
    List<Integer> conversationIds =
        conversations.stream().map(Conversation::getId).collect(Collectors.toList());
    return getConversationDetailsByConversationIds(conversationIds);
  }

  public void markConversationAsRead(Conversation conversation, User user) {
    Integer conversationId = conversation.getId();
    List<ConversationMessage> conversationMessages =
        getConversionDetailRelatedByConversationId(conversationId);
    conversationMessages.forEach(
        conversationMessage -> {
          if (!conversationMessage.getIs_read()
              && conversationMessage.getSender().getId() != user.getId()) {
            conversationMessage.setIs_read(true);
            modelFactoryService.conversationMessageRepository.save(conversationMessage);
          }
        });
  }

  public List<Conversation> getUnreadConversations(User user) {
    List<ConversationMessage> conversationMessages = getConversationMessages(user);
    return conversationMessages.stream()
        .filter(message -> !message.getIs_read() && !message.getSender().equals(user))
        .map(ConversationMessage::getConversation)
        .distinct()
        .toList();
  }
}
