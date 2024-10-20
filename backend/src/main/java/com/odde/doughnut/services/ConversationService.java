package com.odde.doughnut.services;

import com.odde.doughnut.entities.AssessmentQuestionInstance;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationMessage;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.List;
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

  public Conversation startConversationOfNote(Note note, User initiator, String message) {
    Conversation conversation = new Conversation();
    conversation.setNote(note);
    conversation.setConversationInitiator(initiator);
    modelFactoryService.conversationRepository.save(conversation);
    addMessageToConversation(conversation, initiator, message);
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
    conversationMessage.setReadByReceiver(false);
    return modelFactoryService.conversationMessageRepository.save(conversationMessage);
  }

  public List<ConversationMessage> getConversationMessages(User user) {
    List<Conversation> conversations = conversationRelatedToUser(user);
    return conversations.stream()
        .flatMap(conversation -> conversation.getConversationMessages().stream())
        .toList();
  }

  public void markConversationAsRead(Conversation conversation, User user) {
    Integer conversationId = conversation.getId();
    List<ConversationMessage> conversationMessages = conversation.getConversationMessages();
    conversationMessages.forEach(
        conversationMessage -> {
          if (!conversationMessage.getReadByReceiver()
              && conversationMessage.getSender().getId() != user.getId()) {
            conversationMessage.setReadByReceiver(true);
            modelFactoryService.conversationMessageRepository.save(conversationMessage);
          }
        });
  }

  public List<Conversation> getUnreadConversations(User user) {
    List<ConversationMessage> conversationMessages = getConversationMessages(user);
    return conversationMessages.stream()
        .filter(message -> !message.getReadByReceiver() && !message.getSender().equals(user))
        .map(ConversationMessage::getConversation)
        .toList();
  }
}
