package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ConversationStartControllerTest extends ConversationMessageControllerTestBase {

  @Nested
  class StartConversationAboutNote {
    Note note;
    String msg = "This is a feedback sent from note";

    @BeforeEach
    void setup() {
      note = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();
    }

    @Test
    void startsConversationAsCurrentUser() {
      Conversation conversation = controller.startConversationAboutNote(note, msg);

      assertThat(conversation.getConversationInitiator(), equalTo(currentUser.getUser()));
      assertThat(conversationRepository.findAll().iterator().hasNext(), equalTo(true));
    }

    @Test
    void addsInitialMessage() {
      Conversation conversation = controller.startConversationAboutNote(note, msg);
      makeMe.refresh(conversation);

      assertThat(conversation.getConversationMessages(), hasSize(1));
      assertThat(conversation.getConversationMessages().getFirst().getMessage(), equalTo(msg));
    }
  }

  @Nested
  class StartConversationAboutRecallPrompt {
    RecallPrompt recallPrompt;

    @BeforeEach
    void setup() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).please();
      recallPrompt =
          makeMe
              .aRecallPrompt()
              .forMemoryTracker(memoryTracker)
              .withPredefinedQuestionForNote(note)
              .please();
    }

    @Test
    void startsConversationAsCurrentUser() {
      Conversation conversation = controller.startConversationAboutRecallPrompt(recallPrompt);

      assertThat(conversation.getConversationInitiator(), equalTo(currentUser.getUser()));
    }

    @Test
    void setsRecallPromptAsSubject() {
      Conversation conversation = controller.startConversationAboutRecallPrompt(recallPrompt);
      makeMe.refresh(conversation);

      assertThat(conversation.getSubject().getRecallPrompt(), equalTo(recallPrompt));
      assertThat(
          conversation.getSubjectOwnership(), equalTo(recallPrompt.getNotebook().getOwnership()));
    }
  }
}
