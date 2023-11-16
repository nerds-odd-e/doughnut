package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.OpenAIChatGPTFineTuningExample;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.file.File;
import com.theokanning.openai.fine_tuning.FineTuningJob;
import io.reactivex.Single;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.Buffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class AiAdvisorServiceTriggerFineTuningTest {
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired MakeMe makeMe;
  private FineTuningService fineTuningService;
  @Mock private OpenAiApi openAiApi;
  private AiAdvisorService aiAdvisorService;

  @BeforeEach
  void setup() {
    fineTuningService = new FineTuningService(this.modelFactoryService, openAiApi);
    aiAdvisorService = new AiAdvisorService(openAiApi);
  }

  @Nested
  class UploadAndTriggerFineTuning {

    @Test
    void shouldFailWhenNoFeedback() {
      var result =
          assertThrows(
              OpenAIServiceErrorException.class,
              () -> aiAdvisorService.uploadAndTriggerFineTuning(makeExamples(0), "test"));
      assertEquals(result.getMessage(), "Positive feedback cannot be less than 10.");
    }

    @Test
    void whenOpenAiServiceFailShouldGetFailMessage() {
      when(openAiApi.uploadFile(any(RequestBody.class), any(MultipartBody.Part.class)))
          .thenThrow(new RuntimeException());
      var result =
          assertThrows(
              OpenAIServiceErrorException.class,
              () -> aiAdvisorService.uploadAndTriggerFineTuning(makeExamples(10), "test"));
      assertEquals(result.getMessage(), "Upload failed.");
    }

    @Nested
    class SuccessfullyUploaded {
      private List<String> fileContents = new ArrayList<>();

      @BeforeEach
      void setup() {
        File fakeResponse = new File();
        fakeResponse.setId("TestFileId");
        when(openAiApi.uploadFile(any(RequestBody.class), any(MultipartBody.Part.class)))
            .then(
                invocation -> {
                  MultipartBody.Part file = invocation.getArgument(1);
                  fileContents.add(getFileContent(file));
                  return Single.just(fakeResponse);
                });
        FineTuningJob fakeFineTuningResponse = new FineTuningJob();
        fakeFineTuningResponse.setStatus("success");
        when(openAiApi.createFineTuningJob(any())).thenReturn(Single.just(fakeFineTuningResponse));
      }

      @Test
      void shouldPassPositiveExamplesForQuestionGeneration() throws IOException {
        aiAdvisorService.uploadAndTriggerFineTuning(makeExamples(10), "test");
        List<String> lines = fileContents.get(0).lines().toList();
        assertThat(lines, hasSize(10));
        assertThat(lines.get(0), containsString("{\"messages\":["));
      }

      private static String getFileContent(MultipartBody.Part file) {
        RequestBody requestBody = file.body();
        String fileContent = "";

        try {
          Buffer buffer = new Buffer();
          requestBody.writeTo(buffer);
          fileContent = buffer.readUtf8();
        } catch (IOException e) {
          fail("Failed to read the file content");
        }
        return fileContent;
      }
    }
  }

  private List<OpenAIChatGPTFineTuningExample> makeExamples(int count) {
    List<OpenAIChatGPTFineTuningExample> examples = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      examples.add(
          makeMe
              .aQuestionSuggestionForFineTunining()
              .inMemoryPlease()
              .toQuestionGenerationFineTuningExample());
    }
    return examples;
  }
}
