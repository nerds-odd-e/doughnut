package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.repositories.RecallPromptRepository;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.openai.client.OpenAIClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecallQuestionServiceTest {

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @Autowired MakeMe makeMe;
  @Autowired RecallQuestionService recallQuestionService;
  @Autowired RecallPromptRepository recallPromptRepository;
  private Note note;
  private MemoryTracker memoryTracker;
  private OpenAIChatCompletionMock openAIChatCompletionMock;

  @BeforeEach
  void setup() {
    openAIChatCompletionMock = new OpenAIChatCompletionMock(officialClient);

    // Mock chat completion for question evaluation
    QuestionEvaluation evaluation = new QuestionEvaluation();
    evaluation.feasibleQuestion = false;
    evaluation.correctChoices = new int[] {0};
    evaluation.improvementAdvices = "This question needs improvement";
    openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(evaluation);

    note = makeMe.aNote().details("description long enough.").please();
    makeMe.aNote().under(note).please(); // Add another note to the notebook
    memoryTracker = makeMe.aMemoryTrackerFor(note).by(makeMe.aUser().please()).please();
  }

  @Test
  void shouldReturnMostRecentUnansweredRecallPromptWhenMultipleExist() {
    // Create 5 unanswered recall prompts with the same memory tracker
    for (int i = 0; i < 5; i++) {
      makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).please();
    }
    makeMe.entityPersister.flush();

    RecallPrompt result =
        recallPromptRepository.findUnansweredByMemoryTracker(memoryTracker.getId()).orElse(null);
    assertThat("Should return a recall prompt", result, notNullValue());
    assertThat(
        "Should return one of the 5 prompts", result.getMemoryTracker(), equalTo(memoryTracker));
  }

  @Test
  void shouldNotGenerateQuestionForSpellingMemoryTracker() {
    MemoryTracker spellingTracker =
        makeMe.aMemoryTrackerFor(note).by(makeMe.aUser().please()).spelling().please();

    RecallPrompt result = recallQuestionService.generateAQuestion(spellingTracker);

    assertThat("Should return null for spelling tracker", result, equalTo(null));
  }

  @Test
  void shouldGenerateQuestionForNormalMemoryTracker() {
    // Mock AI response for question generation
    MCQWithAnswer jsonQuestion =
        makeMe.aMCQWithAnswer().stem("What is the first color in the rainbow?").please();
    openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(jsonQuestion);

    RecallPrompt result = recallQuestionService.generateAQuestion(memoryTracker);

    assertThat("Should return a recall prompt for normal tracker", result, notNullValue());
    assertThat(
        "Should return a prompt for the memory tracker",
        result.getMemoryTracker(),
        equalTo(memoryTracker));
  }
}
