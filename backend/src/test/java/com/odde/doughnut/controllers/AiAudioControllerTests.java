package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.repositories.GlobalSettingRepository;
import com.odde.doughnut.services.ai.NoteDetailsCompletion;
import com.odde.doughnut.services.ai.OtherAiServices;
import com.odde.doughnut.services.ai.TextFromAudioWithCallInfo;
import com.odde.doughnut.services.openAiApis.OpenAiApiExtended;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.theokanning.openai.completion.chat.ChatMessage;
import io.reactivex.Single;
import java.io.IOException;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AiAudioControllerTests {
  @Autowired MakeMe makeMe;
  @Autowired GlobalSettingRepository globalSettingRepository;
  AiAudioController controller;
  @Mock OpenAiApiExtended openAiApi;
  OpenAIChatCompletionMock openAIChatCompletionMock;

  @BeforeEach
  void commonSetup() {
    initializeController();
    setupMocks();
  }

  private void initializeController() {
    controller =
        new AiAudioController(
            new OtherAiServices(openAiApi), globalSettingRepository, makeMe.entityPersister);
  }

  private void setupMocks() {
    NoteDetailsCompletion completion = new NoteDetailsCompletion(0, "test123");
    openAIChatCompletionMock = new OpenAIChatCompletionMock(openAiApi);
    openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(completion);
    mockTranscriptionSrtResponse("test transcription");
  }

  protected void mockTranscriptionSrtResponse(String responseBody) {
    when(openAiApi.createTranscriptionSrt(any(RequestBody.class)))
        .thenReturn(Single.just(ResponseBody.create(null, responseBody)));
  }

  private MockMultipartFile createMockAudioFile(String filename) {
    return new MockMultipartFile(filename, filename, "audio/mp3", "test".getBytes());
  }

  private AudioUploadDTO createAudioUploadDTO(MockMultipartFile file) {
    var dto = new AudioUploadDTO();
    dto.setUploadAudioFile(file);
    return dto;
  }

  @Nested
  class ConvertAudioToTextTests {
    private AudioUploadDTO audioUploadDTO;

    @BeforeEach
    void setup() {
      audioUploadDTO = createAudioUploadDTO(createMockAudioFile("test.mp3"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"podcast.mp3", "podcast.m4a", "podcast.wav"})
    void convertingFormat(String filename) throws Exception {
      audioUploadDTO.setUploadAudioFile(createMockAudioFile(filename));
      NoteDetailsCompletion result =
          controller
              .audioToText(audioUploadDTO)
              .map(TextFromAudioWithCallInfo::getCompletionFromAudio)
              .orElseThrow();
      assertEquals("test123", result.completion);
    }

    @Test
    void convertAudioToText() throws IOException {
      NoteDetailsCompletion resp =
          controller
              .audioToText(audioUploadDTO)
              .map(TextFromAudioWithCallInfo::getCompletionFromAudio)
              .orElseThrow();
      assertThat(resp.completion, equalTo("test123"));
    }

    @Test
    void shouldTruncateSRTWhenIncomplete() throws IOException {
      audioUploadDTO.setMidSpeech(true);
      mockTranscriptionSrtResponse(
          "1\n00:00:00,000 --> 00:00:03,000\nFirst segment\n\n"
              + "2\n00:00:03,000 --> 00:00:06,000\nSecond segment\n\n"
              + "3\n00:00:06,000 --> 00:00:09,000\nLast segment");

      TextFromAudioWithCallInfo result = controller.audioToText(audioUploadDTO).orElse(null);
      assertNotNull(result);
      assertEquals("00:00:06,000", result.getEndTimestamp());
      assertTrue(result.getRawSRT().contains("First segment"));
      assertTrue(result.getRawSRT().contains("Second segment"));
      assertFalse(result.getRawSRT().contains("Last segment"));
    }

    @Test
    void shouldNotTruncateSRTWhenComplete() throws IOException {
      audioUploadDTO.setMidSpeech(false);
      String fullSRT =
          "1\n00:00:00,000 --> 00:00:03,000\nFirst segment\n\n"
              + "2\n00:00:03,000 --> 00:00:06,000\nSecond segment\n\n"
              + "3\n00:00:06,000 --> 00:00:09,000\nLast segment";
      mockTranscriptionSrtResponse(fullSRT);

      TextFromAudioWithCallInfo result = controller.audioToText(audioUploadDTO).orElse(null);
      assertNotNull(result);
      assertEquals("00:00:09,000", result.getEndTimestamp());
      assertEquals(fullSRT, result.getRawSRT());
    }

    @Test
    void shouldIncludeAdditionalInstructions() throws IOException {
      // Setup
      audioUploadDTO.setAdditionalProcessingInstructions("Translate to Spanish");

      // Execute
      controller.audioToText(audioUploadDTO);

      // Verify
      verify(openAiApi)
          .createChatCompletion(
              argThat(
                  request -> {
                    assertThat(
                        request.getMessages().stream()
                            .filter(m -> "system".equals(m.getRole()))
                            .findFirst()
                            .map(ChatMessage::getTextContent)
                            .orElse(""),
                        containsString("Additional instruction:\nTranslate to Spanish"));
                    return true;
                  }));
    }

    @Test
    void shouldWorkWithoutAdditionalInstructions() throws IOException {
      // Execute
      controller.audioToText(audioUploadDTO);

      // Verify
      verify(openAiApi)
          .createChatCompletion(
              argThat(
                  request -> {
                    boolean hasNoAdditionalInstructions =
                        request.getMessages().stream()
                            .filter(m -> "system".equals(m.getRole()))
                            .noneMatch(m -> m.getTextContent().contains("Additional instruction"));
                    assertTrue(hasNoAdditionalInstructions);
                    return true;
                  }));
    }

    @Test
    void shouldIncludePreviousContentAsUserMessage() throws IOException {
      // Setup
      String previousContent = "Previous text with trailing space ";
      audioUploadDTO.setPreviousNoteDetailsToAppendTo(previousContent);

      // Execute
      controller.audioToText(audioUploadDTO);

      // Verify
      verify(openAiApi)
          .createChatCompletion(
              argThat(
                  request -> {
                    String expectedJson =
                        "{\"previousNoteDetailsToAppendTo\": \"Previous text with trailing space \"}";
                    assertThat(
                        request.getMessages().stream()
                            .filter(m -> "user".equals(m.getRole()))
                            .map(ChatMessage::getTextContent)
                            .anyMatch(
                                content ->
                                    content.contains(
                                        "Previous note details (in JSON format):\n"
                                            + expectedJson)),
                        equalTo(true));
                    return true;
                  }));
    }

    @Test
    void shouldWorkWithoutPreviousContent() throws IOException {
      // Execute
      controller.audioToText(audioUploadDTO);

      // Verify
      verify(openAiApi)
          .createChatCompletion(
              argThat(
                  request -> {
                    boolean hasNoPreviousContent =
                        request.getMessages().stream()
                            .filter(m -> "user".equals(m.getRole()))
                            .noneMatch(
                                m ->
                                    m.getTextContent()
                                        .contains("Previous content (in JSON format):"));
                    assertTrue(hasNoPreviousContent);
                    return true;
                  }));
    }
  }
}
