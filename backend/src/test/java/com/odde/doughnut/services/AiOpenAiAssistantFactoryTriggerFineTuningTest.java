package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.services.ai.OpenAIChatGPTFineTuningExample;
import com.odde.doughnut.services.ai.OtherAiServices;
import com.odde.doughnut.testability.MakeMe;
import com.openai.client.OpenAIClient;
import com.openai.models.files.FileCreateParams;
import com.openai.models.files.FileObject;
import com.openai.models.finetuning.jobs.FineTuningJob;
import com.openai.models.finetuning.jobs.JobCreateParams;
import com.openai.services.blocking.FileService;
import com.theokanning.openai.client.OpenAiApi;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AiOpenAiAssistantFactoryTriggerFineTuningTest {
  @Autowired MakeMe makeMe;

  @MockitoBean(name = "testableOpenAiApi")
  private OpenAiApi openAiApi;

  @MockitoBean(name = "officialOpenAiClient")
  private OpenAIClient officialClient;

  @Autowired OtherAiServices otherAiServices;

  @Nested
  class UploadAndTriggerFineTuning {

    @Test
    void shouldFailWhenNoFeedback() {
      var result =
          assertThrows(
              OpenAIServiceErrorException.class,
              () -> {
                List<OpenAIChatGPTFineTuningExample> examples = makeExamples(0);
                otherAiServices.uploadAndTriggerFineTuning(examples, "test");
              });
      assertEquals(result.getMessage(), "Positive feedback cannot be less than 10.");
    }

    @Test
    void whenOpenAiServiceFailShouldGetFailMessage() {
      FileService fileService = Mockito.mock(FileService.class);
      when(officialClient.files()).thenReturn(fileService);
      when(fileService.create(any(FileCreateParams.class))).thenThrow(new RuntimeException());
      var result =
          assertThrows(
              OpenAIServiceErrorException.class,
              () -> {
                List<OpenAIChatGPTFineTuningExample> examples = makeExamples(10);
                otherAiServices.uploadAndTriggerFineTuning(examples, "test");
              });
      assertEquals(result.getMessage(), "Upload failed.");
    }

    @Nested
    class SuccessfullyUploaded {
      private List<String> fileContents = new ArrayList<>();

      @BeforeEach
      void setup() throws IOException {
        FileService fileService = Mockito.mock(FileService.class);
        when(officialClient.files()).thenReturn(fileService);
        // Use the actual FileObject returned from the API call to get the correct purpose type
        FileObject fakeFileResponse =
            FileObject.builder()
                .id("TestFileId")
                .bytes(0L)
                .createdAt(System.currentTimeMillis() / 1000)
                .filename("test.jsonl")
                .purpose(com.openai.models.files.FileObject.Purpose.FINE_TUNE)
                .status(com.openai.models.files.FileObject.Status.PROCESSED)
                .build();
        ArgumentCaptor<FileCreateParams> paramsCaptor =
            ArgumentCaptor.forClass(FileCreateParams.class);
        when(fileService.create(paramsCaptor.capture()))
            .thenAnswer(
                invocation -> {
                  FileCreateParams params = invocation.getArgument(0);
                  // Extract file content from the Path
                  Object fileParam = params.file();
                  if (fileParam instanceof Path path) {
                    fileContents.add(Files.readString(path));
                  } else if (fileParam instanceof java.io.InputStream inputStream) {
                    fileContents.add(new String(inputStream.readAllBytes()));
                  } else if (fileParam instanceof byte[] bytes) {
                    fileContents.add(new String(bytes));
                  }
                  return fakeFileResponse;
                });
        // Mock official SDK fine-tuning API
        var fineTuningService =
            Mockito.mock(
                com.openai.services.blocking.FineTuningService.class, Mockito.RETURNS_DEEP_STUBS);
        when(officialClient.fineTuning()).thenReturn(fineTuningService);
        FineTuningJob fakeFineTuningResponse =
            Mockito.mock(FineTuningJob.class, Mockito.RETURNS_DEEP_STUBS);
        when(fakeFineTuningResponse.status()).thenReturn(FineTuningJob.Status.SUCCEEDED);
        when(fakeFineTuningResponse.fineTunedModel())
            .thenReturn(java.util.Optional.of("ft:gpt-3.5-turbo-1106:test"));
        when(fineTuningService.jobs().create(any(JobCreateParams.class)))
            .thenReturn(fakeFineTuningResponse);
      }

      @Test
      void shouldPassPositiveExamplesForQuestionGeneration() throws IOException {
        List<OpenAIChatGPTFineTuningExample> examples = makeExamples(10);
        otherAiServices.uploadAndTriggerFineTuning(examples, "test");

        List<String> lines = fileContents.get(0).lines().toList();
        assertThat(lines, hasSize(10));
        assertThat(lines.get(0), containsString("{\"messages\":["));
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
