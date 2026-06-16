package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.is;

import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

/** Regression test using JSONL captured from a live OpenAI batch round-trip (Phase 27). */
@SpringBootTest
@ActiveProfiles("test")
class QuestionGenerationBatchLiveOutputFixtureTest {

  @Autowired OpenAiApiHandler openAiApiHandler;

  @Test
  void parsesCapturedLiveOpenAiBatchSuccessLine() throws IOException {
    String line =
        new ClassPathResource("openai-batch-fixtures/live_batch_success_line.json")
            .getContentAsString(StandardCharsets.UTF_8);

    MCQWithAnswer mcq =
        openAiApiHandler
            .parseStructuredOutputFromBatchSuccessLine(line, MCQWithAnswer.class)
            .orElseThrow();

    assertThat(mcq.isValid(), is(true));
    assertThat(mcq.getQuestion().getQuestionStem(), containsStringIgnoringCase("sunlight"));
    assertThat(mcq.getSolutionChoiceIndex(), is(0));
  }
}
