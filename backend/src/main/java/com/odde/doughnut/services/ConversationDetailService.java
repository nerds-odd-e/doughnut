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

  public ConversationDetail addConversationDetail(
      Conversation conversation, User user, String message) {
    ConversationDetail conversationDetail = new ConversationDetail();
    conversationDetail.setConversation(conversation);
    conversationDetail.setConversationDetailInitiator(user);
    conversationDetail.setMessage(message);
    return modelFactoryService.conversationDetailRepository.save(conversationDetail);
  }

  public List<ConversationDetail> getConversionDetailRelatedByConversationId(int conversationId) {
    return modelFactoryService.conversationDetailRepository.findByConversationInitiator(
        conversationId);
  }
}
