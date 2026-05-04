package com.odde.doughnut.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.controllers.dto.QuestionSuggestionCreationParams;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.doughnut.services.focusContext.FocusContextRetrievalService;
import com.odde.doughnut.services.focusContext.RetrievalConfig;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SuggestedQuestionForFineTuningServiceTest {

  @Autowired MakeMe makeMe;
  @Autowired SuggestedQuestionForFineTuningService suggestedQuestionForFineTuningService;
  @Autowired FocusContextRetrievalService focusContextRetrievalService;
  @Autowired FocusContextMarkdownRenderer focusContextMarkdownRenderer;

  @Test
  void suggestQuestionStoresFocusMarkdownReplayableWithContextSeed() {
    User user = makeMe.aUser().please();
    Note note = makeMe.aNote().creatorAndOwner(user).please();
    long seed = 91_234_567_890L;
    PredefinedQuestion predefinedQuestion =
        makeMe.aPredefinedQuestion().ofAIGeneratedQuestionForNote(note).contextSeed(seed).please();

    SuggestedQuestionForFineTuning entity = new SuggestedQuestionForFineTuning();
    QuestionSuggestionCreationParams params = new QuestionSuggestionCreationParams("ok", true);
    SuggestedQuestionForFineTuning saved =
        suggestedQuestionForFineTuningService.suggestQuestionForFineTuning(
            entity, predefinedQuestion, params, user, new Timestamp(System.currentTimeMillis()));

    RetrievalConfig config = RetrievalConfig.forQuestionGeneration(seed);
    String expected =
        focusContextMarkdownRenderer.render(
            focusContextRetrievalService.retrieve(note, user, config), config);
    assertEquals(expected, saved.getPreservedNoteContent());
  }
}
