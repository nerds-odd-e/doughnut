package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.services.ai.NoteDetailsCompletion;
import com.odde.doughnut.services.ai.TextFromAudioWithCallInfo;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.openai.client.OpenAIClient;
import com.openai.models.audio.transcriptions.TranscriptionCreateParams;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AiAudioControllerTests {
  @Autowired MakeMe makeMe;
  @Autowired AiAudioController controller;

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  OpenAIChatCompletionMock openAIChatCompletionMock;

  @BeforeEach
  void commonSetup() {
    setupMocks();
  }

  private void setupMocks() {
    String patch = "--- a\n+++ b\n@@ -0,0 +1 @@\n+test123\n";
    NoteDetailsCompletion completion = new NoteDetailsCompletion(patch);
    openAIChatCompletionMock = new OpenAIChatCompletionMock(officialClient);
    openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(completion);
    mockTranscriptionSrtResponse("test transcription");
  }

  protected void mockTranscriptionSrtResponse(String responseBody) {
    // Mock the official SDK audio transcription API
    var audioService =
        Mockito.mock(com.openai.services.blocking.AudioService.class, Mockito.RETURNS_DEEP_STUBS);
    when(officialClient.audio()).thenReturn(audioService);
    var transcriptionResponse =
        Mockito.mock(
            com.openai.models.audio.transcriptions.TranscriptionCreateResponse.class,
            Mockito.RETURNS_DEEP_STUBS);
    when(transcriptionResponse.toString()).thenReturn(responseBody);
    when(audioService.transcriptions().create(any(TranscriptionCreateParams.class)))
        .thenReturn(transcriptionResponse);
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
      assertEquals("--- a\n+++ b\n@@ -0,0 +1 @@\n+test123\n", result.patch);
    }

    @Test
    void convertAudioToText() throws IOException {
      NoteDetailsCompletion resp =
          controller
              .audioToText(audioUploadDTO)
              .map(TextFromAudioWithCallInfo::getCompletionFromAudio)
              .orElseThrow();
      assertThat(resp.patch, equalTo("--- a\n+++ b\n@@ -0,0 +1 @@\n+test123\n"));
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
      ArgumentCaptor<com.openai.models.chat.completions.ChatCompletionCreateParams> paramsCaptor =
          ArgumentCaptor.forClass(
              com.openai.models.chat.completions.ChatCompletionCreateParams.class);
      verify(openAIChatCompletionMock.completionService()).create(paramsCaptor.capture());
      boolean hasInstruction =
          paramsCaptor.getValue().messages().stream()
              .map(Object::toString)
              .anyMatch(msg -> msg.contains("Additional instruction:\nTranslate to Spanish"));
      assertTrue(hasInstruction);
    }

    @Test
    void shouldWorkWithoutAdditionalInstructions() throws IOException {
      // Execute
      controller.audioToText(audioUploadDTO);

      // Verify
      ArgumentCaptor<com.openai.models.chat.completions.ChatCompletionCreateParams> paramsCaptor =
          ArgumentCaptor.forClass(
              com.openai.models.chat.completions.ChatCompletionCreateParams.class);
      verify(openAIChatCompletionMock.completionService()).create(paramsCaptor.capture());
      boolean hasNoAdditionalInstructions =
          paramsCaptor.getValue().messages().stream()
              .map(Object::toString)
              .noneMatch(msg -> msg.contains("Additional instruction"));
      assertTrue(hasNoAdditionalInstructions);
    }

    @Test
    void shouldIncludePreviousContentAsUserMessage() throws IOException {
      // Setup
      String previousContent = "Previous text with trailing space ";
      audioUploadDTO.setPreviousNoteDetailsToAppendTo(previousContent);

      // Execute
      controller.audioToText(audioUploadDTO);

      // Verify
      ArgumentCaptor<com.openai.models.chat.completions.ChatCompletionCreateParams> paramsCaptor =
          ArgumentCaptor.forClass(
              com.openai.models.chat.completions.ChatCompletionCreateParams.class);
      verify(openAIChatCompletionMock.completionService()).create(paramsCaptor.capture());
      String expectedJson =
          "{\"previousNoteDetailsToAppendTo\": \"Previous text with trailing space \"}";
      boolean hasPreviousContent =
          paramsCaptor.getValue().messages().stream()
              .map(Object::toString)
              .anyMatch(
                  msg -> msg.contains("Previous note details (in JSON format):\n" + expectedJson));
      assertThat(hasPreviousContent, equalTo(true));
    }

    @Test
    void shouldWorkWithoutPreviousContent() throws IOException {
      // Execute
      controller.audioToText(audioUploadDTO);

      // Verify
      ArgumentCaptor<com.openai.models.chat.completions.ChatCompletionCreateParams> paramsCaptor =
          ArgumentCaptor.forClass(
              com.openai.models.chat.completions.ChatCompletionCreateParams.class);
      verify(openAIChatCompletionMock.completionService()).create(paramsCaptor.capture());
      boolean hasNoPreviousContent =
          paramsCaptor.getValue().messages().stream()
              .map(Object::toString)
              .noneMatch(msg -> msg.contains("Previous content (in JSON format):"));
      assertTrue(hasNoPreviousContent);
    }
  }
}
