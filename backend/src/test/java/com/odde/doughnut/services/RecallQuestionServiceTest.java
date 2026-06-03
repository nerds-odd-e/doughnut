package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
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
  private Note note;
  private MemoryTracker memoryTracker;

  @BeforeEach
  void setup() {
    note = makeMe.aNote().content("description long enough.").please();
    memoryTracker = makeMe.aMemoryTrackerFor(note).by(makeMe.aUser().please()).please();
  }

  @Test
  void shouldReturnMostRecentUnansweredRecallPromptWhenMultipleExist() {
    makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).please();
    RecallPrompt mostRecent = makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).please();
    makeMe.entityPersister.flush();

    RecallPrompt result = recallQuestionService.generateAQuestion(memoryTracker);

    assertThat(result, notNullValue());
    assertThat(result.getId(), equalTo(mostRecent.getId()));
  }
}
