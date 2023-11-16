package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.ChatMessageForFineTuning;
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
import org.jetbrains.annotations.NotNull;
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
class FineTuningServiceTest {
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired MakeMe makeMe;
  private FineTuningService fineTuningService;
  @Mock private OpenAiApi openAiApi;

  @BeforeEach
  void setup() {
    fineTuningService = new FineTuningService(this.modelFactoryService, openAiApi);
  }

  @Nested
  class getAllOpenAIChatGPTFineTuningExample {
    @Test
    void shouldIncludeAllFeedbackData_whenCallGetGoodTrainingData() {
      makeMe.aQuestionSuggestionForFineTunining().positive().please();
      makeMe.aQuestionSuggestionForFineTunining().negative().please();
      assertThat(fineTuningService.getQuestionEvaluationTrainingExamples(), hasSize(2));
    }
  }

  @Nested
  class getAllPositiveFeedbackQuestionGenerationFineTuningExamples {

    @Test
    void shouldReturnNoTrainingDataIfNoMarkedQuestion() {
      List<OpenAIChatGPTFineTuningExample> goodTrainingData =
          fineTuningService.getQuestionGenerationTrainingExamples();
      assertTrue(goodTrainingData.isEmpty());
    }

    @Test
    void shouldReturnGoodTrainingDataIfHavingReadingAuth_whenCallGetGoodTrainingData() {
      Note note = makeMe.aNote().title("Test Topic").please();
      makeMe.aQuestionSuggestionForFineTunining().ofNote(note).positive().please();
      List<OpenAIChatGPTFineTuningExample> goodOpenAIChatGPTFineTuningExampleList =
          fineTuningService.getQuestionGenerationTrainingExamples();
      assertEquals(1, goodOpenAIChatGPTFineTuningExampleList.size());
      List<ChatMessageForFineTuning> goodTrainingData =
          goodOpenAIChatGPTFineTuningExampleList.get(0).getMessages();
      assertThat(goodTrainingData.get(0).getContent(), containsString("Test Topic"));
      assertThat(
          goodTrainingData.get(1).getContent(),
          containsString("assume the role of a Memory Assistant"));
    }

    @Test
    void shouldIncludeTheQuestion_whenCallGetGoodTrainingData() {
      makeMe
          .aQuestionSuggestionForFineTunining()
          .positive()
          .withPreservedQuestion(
              makeMe.aMCQWithAnswer().stem("This is the raw Json question").please())
          .please();
      List<OpenAIChatGPTFineTuningExample> goodOpenAIChatGPTFineTuningExampleList =
          fineTuningService.getQuestionGenerationTrainingExamples();
      List<ChatMessageForFineTuning> goodTrainingData =
          goodOpenAIChatGPTFineTuningExampleList.get(0).getMessages();
      assertThat(
          goodTrainingData.get(2).getFunctionCall().getName(),
          containsString("ask_single_answer_multiple_choice_question"));
      assertThat(
          goodTrainingData.get(2).getFunctionCall().getArguments().toString(),
          containsString("This is the raw Json question"));
    }

    @Test
    void shouldIncludeOnlyPositiveQuestion_whenCallGetGoodTrainingData() {
      makeMe
          .aQuestionSuggestionForFineTunining()
          .negative()
          .withPreservedQuestion(
              makeMe.aMCQWithAnswer().stem("This is the negative raw Json question").please())
          .please();

      List<OpenAIChatGPTFineTuningExample> goodOpenAIChatGPTFineTuningExampleList =
          fineTuningService.getQuestionGenerationTrainingExamples();

      assertEquals(0, goodOpenAIChatGPTFineTuningExampleList.size());
    }
  }

  @Nested
  class UploadAndTriggerFineTuning {

    @Test
    void shouldFailWhenNoFeedback() {
      var result =
          assertThrows(
              OpenAIServiceErrorException.class,
              () -> fineTuningService.uploadDataAndGTriggerFineTuning());
      assertEquals(result.getMessage(), "Positive feedback cannot be less than 10.");
    }

    @Test
    void whenOpenAiServiceFailShouldGetFailMessage() {
      mockPositiveFeedback(10);
      when(openAiApi.uploadFile(any(RequestBody.class), any(MultipartBody.Part.class)))
          .thenThrow(new RuntimeException());
      var result =
          assertThrows(
              OpenAIServiceErrorException.class,
              () -> fineTuningService.uploadDataAndGTriggerFineTuning());
      assertEquals(result.getMessage(), "Upload failed.");
    }

    @Nested
    class SuccessfullyUploaded {
      private List<String> fileContents = new ArrayList<>();

      @BeforeEach
      void setup() {
        mockPositiveFeedback(10);
        mockNegativeFeedback(1);
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
        fineTuningService.uploadDataAndGTriggerFineTuning();
        assertThat(fileContents.get(0).lines().count(), equalTo(10L));
      }

      @Test
      void shouldPassAllExamplesForQuestionGeneration() throws IOException {
        fineTuningService.uploadDataAndGTriggerFineTuning();
        assertThat(fileContents.get(1).lines().count(), equalTo(11L));
      }

      @NotNull
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

  private void mockPositiveFeedback(int count) {
    for (int i = 0; i < count; i++) {
      makeMe.aQuestionSuggestionForFineTunining().positive().please();
    }
  }

  private void mockNegativeFeedback(int count) {
    for (int i = 0; i < count; i++) {
      makeMe.aQuestionSuggestionForFineTunining().negative().please();
    }
  }
}
