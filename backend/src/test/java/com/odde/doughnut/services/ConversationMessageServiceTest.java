package com.odde.doughnut.services;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.controllers.dto.Randomization;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.ChatCompletionConversationService;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.testability.builders.RecallPromptBuilder;
import java.util.List;
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
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired MakeMe makeMe;
  @Autowired AnswerService answerService;
  private ConversationService conversationService;
  private AssessmentService assessmentService;
  private CurrentUser currentUser;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void setup() {
    GlobalSettingsService globalSettingsService =
        new GlobalSettingsService(this.modelFactoryService);
    ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();
    OpenAiApiHandler openAiApiHandler = new OpenAiApiHandler(null);
    ChatCompletionConversationService chatCompletionConversationService =
        new ChatCompletionConversationService(
            openAiApiHandler, globalSettingsService, objectMapper);
    conversationService = new ConversationService(testabilitySettings, this.modelFactoryService);
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
    currentUser = new CurrentUser(makeMe.aUser().please());
    assessmentService =
        new AssessmentService(makeMe.modelFactoryService, testabilitySettings, answerService);
  }

  @Nested
  class conversationActionTest {

    private Notebook notebook;
    private Note topNote;

    @BeforeEach
    void setup() {
      testabilitySettings.setRandomization(new Randomization(Randomization.RandomStrategy.seed, 1));
      topNote =
          makeMe.aHeadNote("OnlineAssessment").creatorAndOwner(currentUser.getUser()).please();
      notebook = topNote.getNotebook();
      notebook.getNotebookSettings().setNumberOfQuestionsInAssessment(1);
    }

    AssessmentAttempt createAssessmentAttempt() {
      makeMe
          .theNote(topNote)
          .withNChildrenThat(1, noteBuilder -> noteBuilder.hasApprovedQuestions(10))
          .please();
      return assessmentService.generateAssessment(notebook, currentUser.getUser());
    }

    @Test
    void shouldAddConversationDetail() {
      Conversation conversation = getConversation();
      String message = "This feedback is wrong";
      ConversationMessage conversationMessage =
          conversationService.addMessageToConversation(
              conversation, currentUser.getUser(), message);
      assertEquals(message, conversationMessage.getMessage());
    }

    @Test
    void shouldReturnListConversationDetail() {
      Conversation conversation = getConversation();
      String message = "This feedback is wrong";
      conversationService.addMessageToConversation(conversation, currentUser.getUser(), message);
      makeMe.refresh(conversation);
      List<ConversationMessage> conversationMessages = conversation.getConversationMessages();
      assertEquals(1, conversationMessages.size());
      assertEquals(message, conversationMessages.getFirst().getMessage());
    }

    private Conversation getConversation() {
      AssessmentAttempt assessmentAttempt = createAssessmentAttempt();
      AssessmentQuestionInstance assessmentQuestionInstance =
          assessmentAttempt.getAssessmentQuestionInstances().getFirst();
      return makeMe
          .aConversation()
          .forAnAssessmentQuestionInstance(assessmentQuestionInstance)
          .from(currentUser.getUser())
          .please();
    }
  }

  @Nested
  class QuestionConversationTest {
    private RecallPrompt recallPrompt;
    private Note note;

    @BeforeEach
    void setup() {
      note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      RecallPromptBuilder recallPromptBuilder = makeMe.aRecallPrompt();
      recallPrompt = recallPromptBuilder.approvedQuestionOf(note).answerChoiceIndex(1).please();
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
