package com.odde.doughnut.services;

import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationDetail;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ConversationDetailService {

  private final ModelFactoryService modelFactoryService;
  private static final int ADMIN_USER = 0;
  private static final int NORMAL_USER = 1;

  public ConversationDetail addConversationDetail(
      Conversation conversation, User user, String message) {
    return addConversationDetail(conversation, user.isAdmin() ? ADMIN_USER : NORMAL_USER, message);
  }

  public ConversationDetail addConversationDetail(
      Conversation conversation, int userType, String message) {
    ConversationDetail conversationDetail = new ConversationDetail();
    conversationDetail.setConversation(conversation);
    conversationDetail.setUserType(userType);
    conversationDetail.setMessage(message);
    return modelFactoryService.conversationDetailRepository.save(conversationDetail);
  }

  public List<ConversationDetail> getConversionDetailRelatedByConversationId(int conversationId) {
    return modelFactoryService.conversationDetailRepository.findByConversationInitiator(
        conversationId);
  }
}
