package com.odde.doughnut.services;

import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ConversationService {

  private final ModelFactoryService modelFactoryService;

  public void startConversation(QuizQuestion quizQuestion, User initiator, String feedback) {
    Conversation conversation = new Conversation();
    conversation.setQuizQuestion(quizQuestion);
    conversation.setNoteCreator(quizQuestion.getPredefinedQuestion().getNote().getCreator());
    conversation.setConversationInitiator(initiator);
    conversation.setMessage(feedback);

    modelFactoryService.conversationRepository.save(conversation);
  }
}
