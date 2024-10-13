package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.Randomization;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
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
class ConversationDetailServiceTest {
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired MakeMe makeMe;
  private ConversationService conversationService;
  private AssessmentService assessmentService;
  private UserModel currentUser;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void setup() {
    conversationService = new ConversationService(this.modelFactoryService);
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
    currentUser = makeMe.aUser().toModelPlease();
    assessmentService = new AssessmentService(makeMe.modelFactoryService, testabilitySettings);
  }

  @Nested
  class conversationActionTest {

    private Notebook notebook;
    private Note topNote;

    @BeforeEach
    void setup() {
      testabilitySettings.setRandomization(new Randomization(Randomization.RandomStrategy.seed, 1));
      topNote = makeMe.aHeadNote("OnlineAssessment").creatorAndOwner(currentUser).please();
      notebook = topNote.getNotebook();
      notebook.getNotebookSettings().setNumberOfQuestionsInAssessment(1);
    }

    AssessmentAttempt createAssessmentAttempt() {
      makeMe
          .theNote(topNote)
          .withNChildrenThat(1, noteBuilder -> noteBuilder.hasApprovedQuestions(10))
          .please();
      return assessmentService.generateAssessment(notebook, currentUser.getEntity());
    }

    @Test
    void shouldReturnEmptyData_whenCallGetConversionDetailRelatedByConversion() {
      int conversationId = 1;
      assertThat(
          conversationService.getConversionDetailRelatedByConversationId(conversationId),
          hasSize(0));
    }

    @Test
    void shouldAddConversationDetail() {
      Conversation conversation = getConversation();
      String message = "This feedback is wrong";
      ConversationDetail conversationDetail =
          conversationService.addConversationDetail(conversation, currentUser.getEntity(), message);
      assertEquals(message, conversationDetail.getMessage());
    }

    @Test
    void shouldReturnListConversationDetail() {
      Conversation conversation = getConversation();
      String message = "This feedback is wrong";
      conversationService.addConversationDetail(conversation, currentUser.getEntity(), message);
      List<ConversationDetail> conversationDetails =
          conversationService.getConversionDetailRelatedByConversationId(conversation.getId());
      assertEquals(1, conversationDetails.size());
      assertEquals(message, conversationDetails.getFirst().getMessage());
    }

    private Conversation getConversation() {
      AssessmentAttempt assessmentAttempt = createAssessmentAttempt();
      AssessmentQuestionInstance assessmentQuestionInstance =
          assessmentAttempt.getAssessmentQuestionInstances().getFirst();
      return makeMe
          .aConversation()
          .forAnAssessmentQuestionInstance(assessmentQuestionInstance)
          .from(currentUser)
          .please();
    }
  }
}
