package com.odde.doughnut.services;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.ConversationMessageRepository;
import com.odde.doughnut.entities.repositories.ConversationRepository;
import com.odde.doughnut.testability.TestabilitySettings;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class ConversationServiceConversationListLimitTest {

  ConversationRepository conversationRepository;
  ConversationService conversationService;

  @BeforeEach
  void setup() {
    conversationRepository = mock(ConversationRepository.class);
    conversationService =
        new ConversationService(
            mock(TestabilitySettings.class),
            conversationRepository,
            mock(ConversationMessageRepository.class));
  }

  @Test
  void shouldRequestAtMostFiftyConversations() {
    User user = new User();
    PageRequest firstPage = PageRequest.of(0, 50);
    when(conversationRepository.findByUserInSubjectOwnershipOrConversationInitiator(
            user, firstPage))
        .thenReturn(List.of());

    conversationService.conversationListForUser(user);

    verify(conversationRepository)
        .findByUserInSubjectOwnershipOrConversationInitiator(user, firstPage);
  }
}
