package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.controllers.dto.AnswerSpellingDTO;
import com.odde.doughnut.controllers.dto.Randomization;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.testability.OpenAiStructuredResponseMock;
import com.openai.client.OpenAIClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

abstract class RecallPromptControllerTestBase extends ControllerTestBase {
  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @Autowired RecallPromptController controller;

  OpenAiStructuredResponseMock openAiStructuredResponseMock;

  @BeforeEach
  void setupRecallPromptBase() {
    currentUser.setUser(makeMe.aUser().please());
    testabilitySettings.setRandomization(new Randomization(Randomization.RandomStrategy.first, 1));
    openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);

    QuestionEvaluation evaluation = new QuestionEvaluation();
    evaluation.feasibleQuestion = false;
    evaluation.correctChoices = new int[] {0};
    evaluation.improvementAdvices = "This question needs improvement";
    openAiStructuredResponseMock.stubStructuredResponse(evaluation);
  }

  Note ownedNote() {
    return makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
  }

  MemoryTracker ownedTracker(Note note) {
    return makeMe.aMemoryTrackerFor(note).forgettingCurveAndNextRecallAt(200.0f).please();
  }

  MemoryTracker ownedSpellingTracker(Note note) {
    return makeMe
        .aMemoryTrackerFor(note)
        .forgettingCurveAndNextRecallAt(200.0f)
        .spelling()
        .please();
  }

  MemoryTracker ownedSpellingTracker() {
    return ownedSpellingTracker(ownedNote());
  }

  RecallPrompt mcqPrompt(MemoryTracker tracker, Note note) {
    MCQWithAnswer mcq = makeMe.aMCQWithAnswer().please();
    return makeMe
        .aRecallPrompt()
        .forMemoryTracker(tracker)
        .ofAIGeneratedQuestion(mcq, note)
        .please();
  }

  RecallPrompt spellingPrompt(MemoryTracker tracker) {
    return makeMe.aRecallPrompt().forMemoryTracker(tracker).spelling().please();
  }

  MemoryTracker memoryTrackerOwnedByAnotherUser() {
    return makeMe.aMemoryTrackerBy(makeMe.aUser().please()).please();
  }

  AnswerDTO choiceAnswer(int choiceIndex) {
    AnswerDTO dto = new AnswerDTO();
    dto.setChoiceIndex(choiceIndex);
    return dto;
  }

  AnswerSpellingDTO spellingAnswer(String answer) {
    AnswerSpellingDTO dto = new AnswerSpellingDTO();
    dto.setSpellingAnswer(answer);
    return dto;
  }
}
