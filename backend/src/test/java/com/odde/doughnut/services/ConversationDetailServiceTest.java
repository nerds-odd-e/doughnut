package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.Randomization;
import com.odde.doughnut.entities.AssessmentAttempt;
import com.odde.doughnut.entities.AssessmentQuestionInstance;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConversationDetailServiceTest {
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired MakeMe makeMe;
  private ConversationDetailService conversationDetailService;
  private AssessmentService assessmentService;
  private UserModel currentUser;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void setup() {
    conversationDetailService = new ConversationDetailService(this.modelFactoryService);
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
    currentUser = makeMe.aUser().toModelPlease();
    assessmentService = new AssessmentService(makeMe.modelFactoryService, testabilitySettings);
  }

  @Nested
  class getAllOpenAIChatGPTFineTuningExample {

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
          conversationDetailService.getConversionDetailRelatedByConversion(conversationId),
          hasSize(0));
    }

    @Nested
    class conversationDetailTest {

      @Test
      void shouldReturnData_whenCallGetConversionDetailRelatedByConversion() {
        AssessmentAttempt assessmentAttempt = createAssessmentAttempt();
        AssessmentQuestionInstance assessmentQuestionInstance =
            assessmentAttempt.getAssessmentQuestionInstances().getFirst();
        makeMe
            .aConversation()
            .forAnAssessmentQuestionInstance(assessmentQuestionInstance)
            .from(currentUser)
            .please();
        assertNotNull(assessmentAttempt);
      }
    }
  }
}
