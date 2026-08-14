package com.odde.doughnut.entities.repositories;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AnswerRepositoryTest {

  @Autowired MakeMe makeMe;
  @Autowired AnswerRepository answerRepository;

  @Test
  void optionalConfusionAdjustedTrackerSurvivesReloadAndIsAbsentOnOrdinaryAnswers() {
    MemoryTracker adjusted = makeMe.aMemoryTrackerFor(makeMe.aNote().please()).please();
    MemoryTracker prompted = makeMe.aMemoryTrackerFor(makeMe.aNote().please()).please();

    Integer linkedAnswerId =
        makeMe
            .aRecallPrompt()
            .spelling()
            .forMemoryTracker(prompted)
            .answerSpelling("x")
            .confusionAdjusted(adjusted)
            .please()
            .getAnswer()
            .getId();
    Integer ordinaryAnswerId =
        makeMe
            .aRecallPrompt()
            .spelling()
            .forMemoryTracker(prompted)
            .answerSpelling("y")
            .please()
            .getAnswer()
            .getId();

    makeMe.entityPersister.flushAndClear();

    Answer reloadedLinked = answerRepository.findById(linkedAnswerId).orElseThrow();
    assertThat(
        reloadedLinked.getConfusionAdjustedMemoryTracker().getId(), equalTo(adjusted.getId()));

    Answer reloadedOrdinary = answerRepository.findById(ordinaryAnswerId).orElseThrow();
    assertThat(reloadedOrdinary.getConfusionAdjustedMemoryTracker(), nullValue());
  }
}
