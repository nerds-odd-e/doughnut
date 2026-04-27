package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.ConversationListItem;
import com.odde.doughnut.entities.AssessmentQuestionInstance;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationMessage;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Ownership;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.ConversationMessageRepository;
import com.odde.doughnut.entities.repositories.ConversationRepository;
import com.odde.doughnut.testability.TestabilitySettings;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ConversationService {

  private final TestabilitySettings testabilitySettings;
  private final ConversationRepository conversationRepository;
  private final ConversationMessageRepository conversationMessageRepository;

  private Conversation initializeConversation(User initiator) {
    Conversation conversation = new Conversation();
    conversation.setConversationInitiator(initiator);
    return conversation;
  }

  public Conversation startConversationAboutRecallPrompt(
      AssessmentQuestionInstance assessmentQuestionInstance, User initiator) {
    Conversation conversation = initializeConversation(initiator);
    conversation.setAssessmentQuestionInstance(assessmentQuestionInstance);
    return conversationRepository.save(conversation);
  }

  public Conversation startConversationAboutRecallPrompt(
      RecallPrompt recallPrompt, User initiator) {
    Conversation conversation = initializeConversation(initiator);
    conversation.setRecallPrompt(recallPrompt);
    conversationRepository.save(conversation);
    return conversation;
  }

  public Conversation startConversationOfNote(Note note, User initiator, String message) {
    Conversation conversation = initializeConversation(initiator);
    conversation.setNote(note);
    conversationRepository.save(conversation);
    addMessageToConversation(conversation, initiator, message);
    return conversation;
  }

  public List<Conversation> conversationRelatedToUser(User user) {
    return conversationRepository.findByUserInSubjectOwnershipOrConversationInitiator(
        user, Pageable.unpaged());
  }

  public List<ConversationListItem> conversationListForUser(User user) {
    return conversationRepository
        .findByUserInSubjectOwnershipOrConversationInitiator(user, PageRequest.of(0, 50))
        .stream()
        .map(conversation -> toListItem(conversation, user))
        .toList();
  }

  private ConversationListItem toListItem(Conversation conversation, User currentUser) {
    return new ConversationListItem(
        conversation.getId(),
        getConversationSubject(conversation),
        partnerNameForList(conversation, currentUser));
  }

  private String partnerNameForList(Conversation conversation, User currentUser) {
    User initiator = conversation.getConversationInitiator();
    if (initiator != null && !Objects.equals(initiator.getId(), currentUser.getId())) {
      return initiator.getName();
    }
    Ownership ownership = conversation.getSubjectOwnership();
    if (ownership != null && ownership.getCircle() != null) {
      return ownership.getCircle().getName();
    }
    if (ownership != null) {
      return ownership.getOwnerName();
    }
    return null;
  }

  public ConversationMessage addMessageToConversation(
      Conversation conversation, User user, String message) {
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    ConversationMessage conversationMessage = new ConversationMessage();
    conversationMessage.setConversation(conversation);
    conversation.getConversationMessages().add(conversationMessage); // for in memory consistency
    conversationMessage.setSender(user);
    conversationMessage.setMessage(message);
    conversationMessage.setCreatedAt(currentUTCTimestamp);

    return conversationMessageRepository.save(conversationMessage);
  }

  public void markConversationAsRead(Conversation conversation, User user) {
    conversation
        .getConversationMessages()
        .forEach(
            conversationMessage -> {
              if (!conversationMessage.getReadByReceiver()
                  && !Objects.equals(conversationMessage.getSender(), user)) {
                conversationMessage.setReadByReceiver(true);
                conversationMessageRepository.save(conversationMessage);
              }
            });
  }

  public List<ConversationMessage> getUnreadConversations(User user) {
    return conversationRepository.findUnreadMessagesByUser(user);
  }

  public List<Conversation> getConversationsAboutNote(Note note, User entity) {
    return conversationRelatedToUser(entity).stream()
        .filter(conversation -> note.equals(conversation.getSubject().getNote()))
        .toList();
  }

  public String getConversationSubject(Conversation conversation) {
    Note note = conversation.getSubject().getNote();
    if (note != null) {
      return note.getTitle();
    }

    AssessmentQuestionInstance assessmentQuestionInstance =
        conversation.getSubject().getAssessmentQuestionInstance();
    if (assessmentQuestionInstance != null) {
      return assessmentQuestionInstance.getMultipleChoicesQuestion().getF0__stem();
    }

    RecallPrompt recallPrompt = conversation.getSubject().getRecallPrompt();
    if (recallPrompt != null && recallPrompt.getPredefinedQuestion() != null) {
      return recallPrompt.getPredefinedQuestion().getNote().getTitle();
    }

    return "Unknown Subject";
  }
}
