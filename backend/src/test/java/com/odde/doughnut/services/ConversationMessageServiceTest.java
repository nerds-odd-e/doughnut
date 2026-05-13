package com.odde.doughnut.services;

import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.testability.builders.NoteBuilder;
import com.odde.doughnut.testability.builders.RecallPromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConversationMessageServiceTest {
  @Autowired MakeMe makeMe;
  @Autowired TestabilitySettings testabilitySettings;
  @Autowired ConversationService conversationService;
  private CurrentUser currentUser;

  @BeforeEach
  void setup() {
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
    currentUser = new CurrentUser(makeMe.aUser().please());
  }

  @Nested
  class QuestionConversationTest {
    private RecallPrompt recallPrompt;
    private Note note;

    @BeforeEach
    void setup() {
      NoteBuilder noteBuilder = makeMe.aNote();
      note = noteBuilder.nbCreatorAndOwner(currentUser.getUser()).please();
      RecallPromptBuilder recallPromptBuilder = makeMe.aRecallPrompt();
      recallPrompt =
          recallPromptBuilder.withPredefinedQuestionForNote(note).answerChoiceIndex(1).please();
    }

    @Test
    void shouldSetCorrectOwnershipAndSubject() {
      Conversation conversation =
          conversationService.startConversationAboutRecallPrompt(
              recallPrompt, currentUser.getUser());

      makeMe.refresh(conversation);
      assertEquals(recallPrompt, conversation.getSubject().getRecallPrompt());
      assertEquals(note.getNotebook().getOwnership(), conversation.getSubjectOwnership());
      assertEquals(currentUser.getUser(), conversation.getConversationInitiator());
    }
  }
}
