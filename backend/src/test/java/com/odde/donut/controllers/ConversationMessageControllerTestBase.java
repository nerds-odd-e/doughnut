package com.odde.donut.controllers;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import com.odde.donut.entities.repositories.ConversationRepository;
import com.openai.models.responses.ResponseCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

abstract class ConversationMessageControllerTestBase extends ControllerTestBase {
  @Autowired ConversationMessageController controller;
  @Autowired ConversationRepository conversationRepository;

  @BeforeEach
  void setupCurrentUser() {
    currentUser.setUser(makeMe.aUser().please());
  }

  protected ResponseCreateParams lastRequestSentToAi() {
    ArgumentCaptor<ResponseCreateParams> captor =
        ArgumentCaptor.forClass(ResponseCreateParams.class);
    verify(officialClient.async().responses(), atLeastOnce()).createStreaming(captor.capture());
    return captor.getValue();
  }
}
