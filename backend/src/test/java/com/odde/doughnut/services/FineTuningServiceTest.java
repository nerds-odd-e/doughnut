package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.OpenAIChatGPTFineTuningExample;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.file.File;
import io.reactivex.Single;
import java.io.IOException;
import java.util.List;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
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
    void shouldSuccessWhen10FeedbackAndUploadFile() {
      mockFeedback(11);
      File fakeResponse = new File();
      fakeResponse.setId("TestFileId");
      when(openAiApi.uploadFile(any(RequestBody.class), any(MultipartBody.Part.class)))
          .thenReturn(Single.just(fakeResponse));
      assertDoesNotThrow(
          () -> {
            fineTuningService.uploadFineTuningExamples();
          });
    }

    @Test
    void shouldFailWhenNoFeedback() {
      var result =
          assertThrows(
              OpenAIServiceErrorException.class,
              () -> fineTuningService.uploadFineTuningExamples());
      assertEquals(result.getMessage(), "Positive feedback cannot be less than 10.");
    }

    @Test
    void whenOpenAiServiceFailShouldGetFailMessage() throws IOException {
      mockFeedback(10);
      when(openAiApi.uploadFile(any(RequestBody.class), any(MultipartBody.Part.class)))
          .thenThrow(new RuntimeException());
      var result =
          assertThrows(
              OpenAIServiceErrorException.class,
              () -> {
                fineTuningService.uploadFineTuningExamples();
              });
      assertEquals(result.getMessage(), "Upload failed.");
    }

    @Test
    void shouldReturnGoodTrainingDataIfHavingReadingAuth_whenCallGetGoodTrainingData() {
      Note note = makeMe.aNote().title("Test Topic").please();
      makeMe.aQuestionSuggestionForFineTunining().ofNote(note).positive().please();
      List<OpenAIChatGPTFineTuningExample> goodOpenAIChatGPTFineTuningExampleList =
          fineTuningService.getQuestionGenerationTrainingExamples();
      assertEquals(1, goodOpenAIChatGPTFineTuningExampleList.size());
      List<ChatMessage> goodTrainingData =
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
      List<ChatMessage> goodTrainingData =
          goodOpenAIChatGPTFineTuningExampleList.get(0).getMessages();
      assertThat(
          goodTrainingData.get(2).getContent(), containsString("This is the raw Json question"));
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
  class getGoodOpenAIChatGPTFineTuningExample {

    @Test
    void shouldSuccessWhen10FeedbackAndUploadFile() {
      mockFeedback(11);
      File fakeResponse = new File();
      fakeResponse.setId("TestFileId");
      when(openAiApi.uploadFile(any(RequestBody.class), any(MultipartBody.Part.class)))
          .thenReturn(Single.just(fakeResponse));
      assertDoesNotThrow(() -> fineTuningService.uploadFineTuningExamples());
    }

    @Test
    void shouldFailWhenNoFeedback() {
      var result =
          assertThrows(
              OpenAIServiceErrorException.class,
              () -> fineTuningService.uploadFineTuningExamples());
      assertEquals(result.getMessage(), "Positive feedback cannot be less than 10.");
    }

    @Test
    void whenOpenAiServiceFailShouldGetFailMessage() {
      mockFeedback(10);
      when(openAiApi.uploadFile(any(RequestBody.class), any(MultipartBody.Part.class)))
          .thenThrow(new RuntimeException());
      var result =
          assertThrows(
              OpenAIServiceErrorException.class,
              () -> fineTuningService.uploadFineTuningExamples());
      assertEquals(result.getMessage(), "Upload failed.");
    }
  }

  private void mockFeedback(int count) {
    for (int i = 0; i < count; i++) {
      makeMe.aQuestionSuggestionForFineTunining().positive().please();
    }
  }
}
