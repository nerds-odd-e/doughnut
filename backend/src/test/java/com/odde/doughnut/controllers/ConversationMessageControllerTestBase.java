package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.repositories.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

abstract class ConversationMessageControllerTestBase extends ControllerTestBase {
  @Autowired ConversationMessageController controller;
  @Autowired ConversationRepository conversationRepository;

  @BeforeEach
  void setupCurrentUser() {
    currentUser.setUser(makeMe.aUser().please());
  }
}
