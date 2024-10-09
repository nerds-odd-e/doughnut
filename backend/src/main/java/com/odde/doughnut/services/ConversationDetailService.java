package com.odde.doughnut.services;

import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationDetail;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ConversationDetailService {

  private final ModelFactoryService modelFactoryService;

  public ConversationDetail startConversationDetail(
      Conversation conversation, int userType, String feedback) {
    ConversationDetail conversationDetail = new ConversationDetail();
    conversationDetail.setConversation(conversation);
    conversationDetail.setUserType(userType);
    conversationDetail.setMessage(feedback);
    return modelFactoryService.conversationDetailRepository.save(conversationDetail);
  }

  public List<ConversationDetail> getConversionDetailRelatedByConversion(int conversationId) {
    return modelFactoryService.conversationDetailRepository.findByConversationInitiator(
        conversationId);
  }
}
