package com.odde.doughnut.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.odde.doughnut.entities.AssessmentQuestionInstance;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationMessage;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewQuestionInstance;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.testability.TestabilitySettings;
import jakarta.annotation.Resource;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ConversationService {

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private final ModelFactoryService modelFactoryService;

  public Conversation startConversation(
      AssessmentQuestionInstance assessmentQuestionInstance, User initiator) {
    Conversation conversation = new Conversation();
    conversation.setAssessmentQuestionInstance(assessmentQuestionInstance);
    conversation.setConversationInitiator(initiator);
    modelFactoryService.conversationRepository.save(conversation);
    return conversation;
  }

  public Conversation startConversation(
      ReviewQuestionInstance reviewQuestionInstance, User initiator) {
    Conversation conversation = new Conversation();
    conversation.setReviewQuestionInstance(reviewQuestionInstance);
    conversation.setConversationInitiator(initiator);
    modelFactoryService.conversationRepository.save(conversation);

    // Create initial message with question details
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode questionDetails = mapper.createObjectNode();
    questionDetails.put("question", reviewQuestionInstance.getBareQuestion().toString());
    questionDetails.put(
        "correctAnswerIndex",
        reviewQuestionInstance.getPredefinedQuestion().getCorrectAnswerIndex());

    // Only add answer details if an answer exists
    if (reviewQuestionInstance.getAnswer() != null) {
      questionDetails.put("userAnswer", reviewQuestionInstance.getAnswer().getChoiceIndex());
      questionDetails.put("isCorrect", reviewQuestionInstance.getAnswer().getCorrect());
    }

    addMessageToConversation(
        conversation,
        null, // null user indicates system message
        questionDetails.toPrettyString());

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
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    ConversationMessage conversationMessage = new ConversationMessage();
    conversationMessage.setConversation(conversation);
    conversation.getConversationMessages().add(conversationMessage); // for in memory consistency
    conversationMessage.setSender(user);
    conversationMessage.setMessage(message);
    conversationMessage.setCreatedAt(currentUTCTimestamp);

    // Update sync timestamp when AI sends a message
    if (user == null) { // AI message
      conversation.setLastAiAssistantThreadSync(currentUTCTimestamp);
      modelFactoryService.conversationRepository.save(conversation);
    }

    return modelFactoryService.conversationMessageRepository.save(conversationMessage);
  }

  public void markConversationAsRead(Conversation conversation, User user) {
    conversation
        .getConversationMessages()
        .forEach(
            conversationMessage -> {
              if (!conversationMessage.getReadByReceiver()
                  && !Objects.equals(conversationMessage.getSender(), user)) {
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

  public void setConversationAiAssistantThreadId(Conversation conversation, String threadId) {
    conversation.setAiAssistantThreadId(threadId);
    modelFactoryService.save(conversation);
  }

  public void updateLastAiAssistantThreadSync(Conversation conversation) {
    conversation.setLastAiAssistantThreadSync(testabilitySettings.getCurrentUTCTimestamp());
    modelFactoryService.save(conversation);
  }
}
