package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.FeedbackDTO;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.QuizQuestionAndAnswer;
import org.springframework.stereotype.Service;

@Service
public class ConversationService {

  public Conversation startConversation(
      QuizQuestionAndAnswer quizQuestionAndAnswer, FeedbackDTO feedbackDTO) {

    Conversation conversation = new Conversation();
    conversation.setQuizQuestionAndAnswer(quizQuestionAndAnswer);
    conversation.setNoteCreator(quizQuestionAndAnswer.getNote().getCreator());

    return conversation;
  }
}
