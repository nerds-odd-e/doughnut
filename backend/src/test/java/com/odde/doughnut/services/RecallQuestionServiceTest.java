package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.repositories.RecallPromptRepository;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecallQuestionServiceTest {

  @Autowired MakeMe makeMe;
  @Autowired RecallQuestionService recallQuestionService;
  @Autowired RecallPromptRepository recallPromptRepository;
  private Note note;
  private MemoryTracker memoryTracker;

  @BeforeEach
  void setup() {
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
}
