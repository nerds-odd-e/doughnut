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
    conversation.getConversationMessages().add(conversationMessage); // for in memory consistency
    conversationMessage.setSender(user);
    conversationMessage.setMessage(message);
    return modelFactoryService.conversationMessageRepository.save(conversationMessage);
  }

  public void markConversationAsRead(Conversation conversation, User user) {
    conversation
        .getConversationMessages()
        .forEach(
            conversationMessage -> {
              if (!conversationMessage.getReadByReceiver()
                  && !conversationMessage.getSender().equals(user)) {
                conversationMessage.setReadByReceiver(true);
                modelFactoryService.conversationMessageRepository.save(conversationMessage);
              }
            });
  }

  public List<ConversationMessage> getUnreadConversations(User user) {
    return modelFactoryService.conversationRepository.findUnreadMessagesByUser(user);
  }

  public List<Conversation> getConversationsAboutNote(Note note, User entity) {
    return conversationRelatedToUser(entity).stream()
        .filter(conversation -> note.equals(conversation.getSubject().getNote()))
        .toList();
  }
}
