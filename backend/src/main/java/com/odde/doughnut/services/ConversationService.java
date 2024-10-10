package com.odde.doughnut.services;

import com.odde.doughnut.entities.AssessmentQuestionInstance;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationDetail;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ConversationService {

  private final ModelFactoryService modelFactoryService;
  private static final int ADMIN_USER = 0;
  private static final int NORMAL_USER = 1;

  public Conversation startConversation(
      AssessmentQuestionInstance assessmentQuestionInstance, User initiator, String feedback) {
    Conversation conversation = new Conversation();
    conversation.setAssessmentQuestionInstance(assessmentQuestionInstance);
    conversation.setConversationInitiator(initiator);
    conversation.setMessage(feedback);
    modelFactoryService.conversationRepository.save(conversation);

    int userType = initiator.isAdmin() ? ADMIN_USER : NORMAL_USER;
    ConversationDetail conversationDetail = new ConversationDetail();
    conversationDetail.setConversation(conversation);
    conversationDetail.setUserType(userType);
    conversationDetail.setMessage(feedback);
    modelFactoryService.conversationDetailRepository.save(conversationDetail);
    return conversation;
  }

  public List<Conversation> conversationRelatedToUser(User user) {
    return modelFactoryService.conversationRepository
        .findByUserInSubjectOwnershipOrConversationInitiator(user);
  }
}
