package com.odde.doughnut.services;

import com.odde.doughnut.entities.AssessmentQuestionInstance;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationMessage;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.ChatCompletionConversationService;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.completion.chat.AssistantMessage;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.SystemMessage;
import com.theokanning.openai.completion.chat.UserMessage;
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
  private final ChatCompletionConversationService chatCompletionConversationService;

  private Conversation initializeConversation(User initiator) {
    Conversation conversation = new Conversation();
    conversation.setConversationInitiator(initiator);
    return conversation;
  }

  public Conversation startConversationAboutRecallPrompt(
      AssessmentQuestionInstance assessmentQuestionInstance, User initiator) {
    Conversation conversation = initializeConversation(initiator);
    conversation.setAssessmentQuestionInstance(assessmentQuestionInstance);
    return modelFactoryService.conversationRepository.save(conversation);
  }

  public Conversation startConversationAboutRecallPrompt(
      RecallPrompt recallPrompt, User initiator) {
    Conversation conversation = initializeConversation(initiator);
    conversation.setRecallPrompt(recallPrompt);
    modelFactoryService.conversationRepository.save(conversation);
    return conversation;
  }

  public Conversation startConversationOfNote(Note note, User initiator, String message) {
    Conversation conversation = initializeConversation(initiator);
    conversation.setNote(note);
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

  public static String exportChatCompletionRequestForChatGPT(
      ChatCompletionRequest request, String title) {
    StringBuilder export = new StringBuilder();

    // Title
    export.append("# Conversation: ").append(title).append("\n\n");

    // Context section: Extract all system messages
    export.append("## Context\n\n");
    List<ChatMessage> messages = request.getMessages();
    for (ChatMessage message : messages) {
      if (message instanceof SystemMessage systemMessage) {
        export.append(systemMessage.getTextContent()).append("\n\n");
      }
    }

    // Conversation History: Extract user and assistant messages
    export.append("## Conversation History\n\n");
    for (ChatMessage message : messages) {
      if (message instanceof UserMessage userMessage) {
        export
            .append("**User**: ")
            .append(formatMessage(userMessage.getTextContent()))
            .append("\n");
      } else if (message instanceof AssistantMessage assistantMessage) {
        export
            .append("**Assistant**: ")
            .append(formatMessage(assistantMessage.getTextContent()))
            .append("\n");
      }
    }

    return export.toString();
  }

  public String exportConversationForChatGPT(Conversation conversation) {
    ChatCompletionRequest request =
        chatCompletionConversationService.buildChatCompletionRequest(conversation);
    String title = getConversationSubject(conversation);
    return exportChatCompletionRequestForChatGPT(request, title);
  }

  private static String formatMessage(String message) {
    // Remove leading and trailing quotes and trim
    return message.replaceAll("^\"|\"$", "").trim();
  }

  private String getConversationSubject(Conversation conversation) {
    Note note = conversation.getSubject().getNote();
    if (note != null) {
      return note.getTopicConstructor();
    }

    AssessmentQuestionInstance assessmentQuestionInstance =
        conversation.getSubject().getAssessmentQuestionInstance();
    if (assessmentQuestionInstance != null) {
      return assessmentQuestionInstance.getMultipleChoicesQuestion().getStem();
    }

    RecallPrompt recallPrompt = conversation.getSubject().getRecallPrompt();
    if (recallPrompt != null) {
      return recallPrompt.getPredefinedQuestion().getNote().getTopicConstructor();
    }

    return "Unknown Subject";
  }
}
