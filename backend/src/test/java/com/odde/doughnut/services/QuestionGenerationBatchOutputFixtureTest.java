package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.is;

import com.odde.doughnut.services.ai.GeneratedMcq;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

/** Regression test for OpenAI batch success-line parsing using the semantic MCQ contract. */
@SpringBootTest
@ActiveProfiles("test")
class QuestionGenerationBatchOutputFixtureTest {

  @Autowired OpenAiApiHandler openAiApiHandler;

  @Test
  void parsesSemanticMcqBatchSuccessLine() throws IOException {
    String line =
        new ClassPathResource("openai-batch-fixtures/semantic_mcq_batch_success_line.json")
            .getContentAsString(StandardCharsets.UTF_8);

    GeneratedMcq mcq =
        openAiApiHandler
            .parseStructuredOutputFromBatchSuccessLine(line, GeneratedMcq.class)
            .orElseThrow();

    assertThat(mcq.isValid(), is(true));
    assertThat(mcq.getQuestionStem(), containsStringIgnoringCase("sunlight"));
    assertThat(mcq.getCorrectAnswer(), is("Photosynthesis"));
  }
}
