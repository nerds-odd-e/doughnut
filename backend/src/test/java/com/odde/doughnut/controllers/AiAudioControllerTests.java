package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.services.ai.NoteContentCompletion;
import com.odde.doughnut.services.ai.TextFromAudioWithCallInfo;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAiStructuredResponseMock;
import com.openai.client.OpenAIClient;
import com.openai.models.audio.transcriptions.TranscriptionCreateParams;
import com.openai.models.responses.ResponseTextConfig;
import com.openai.models.responses.StructuredResponseCreateParams;
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

  OpenAiStructuredResponseMock openAiStructuredResponseMock;

  @BeforeEach
  void commonSetup() {
    setupMocks();
  }

  private void setupMocks() {
    String transcriptText = "test123";
    NoteContentCompletion completion = new NoteContentCompletion(transcriptText);
    openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);
    openAiStructuredResponseMock.stubStructuredResponse(completion);
    mockTranscriptionSrtResponse("test transcription");
  }

  protected void mockTranscriptionSrtResponse(String responseBody) {
    var audioService =
        Mockito.mock(com.openai.services.blocking.AudioService.class, Mockito.RETURNS_DEEP_STUBS);
    when(officialClient.audio()).thenReturn(audioService);
    var transcriptionResponse =
        com.openai.models.audio.transcriptions.TranscriptionCreateResponse.ofTranscription(
            com.openai.models.audio.transcriptions.Transcription.builder()
                .text(responseBody)
                .build());
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
      NoteContentCompletion result =
          controller
              .audioToText(audioUploadDTO)
              .map(TextFromAudioWithCallInfo::getCompletionFromAudio)
              .orElseThrow();
      assertEquals("test123", result.content);
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
      audioUploadDTO.setAdditionalProcessingInstructions("Translate to Spanish");

      controller.audioToText(audioUploadDTO);

      @SuppressWarnings({"unchecked", "rawtypes"})
      ArgumentCaptor<StructuredResponseCreateParams<NoteContentCompletion>> paramsCaptor =
          ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
      verify(openAiStructuredResponseMock.responseService()).create(paramsCaptor.capture());
      StructuredResponseCreateParams<NoteContentCompletion> params = paramsCaptor.getValue();
      String instructions = params.rawParams().instructions().orElse("");
      assertTrue(instructions.contains("Additional instruction:\nTranslate to Spanish"));
      assertThat(
          "Should use Responses structured text format",
          params.rawParams().text().flatMap(ResponseTextConfig::format).isPresent(),
          is(true));
    }

    @Test
    void shouldIncludePreviousContentAsUserMessage() throws IOException {
      String previousContent = "Previous text with trailing space ";
      audioUploadDTO.setPreviousNoteContentToAppendTo(previousContent);

      controller.audioToText(audioUploadDTO);

      @SuppressWarnings({"unchecked", "rawtypes"})
      ArgumentCaptor<StructuredResponseCreateParams<NoteContentCompletion>> paramsCaptor =
          ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
      verify(openAiStructuredResponseMock.responseService()).create(paramsCaptor.capture());
      String expectedJson =
          "{\"previousNoteContentToAppendTo\": \"Previous text with trailing space \"}";
      String input = paramsCaptor.getValue().rawParams().input().flatMap(i -> i.text()).orElse("");
      assertThat(input, equalTo("Previous note content (in JSON format):\n" + expectedJson));
    }

    @Test
    void shouldWorkWithoutPreviousContent() throws IOException {
      controller.audioToText(audioUploadDTO);

      @SuppressWarnings({"unchecked", "rawtypes"})
      ArgumentCaptor<StructuredResponseCreateParams<NoteContentCompletion>> paramsCaptor =
          ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
      verify(openAiStructuredResponseMock.responseService()).create(paramsCaptor.capture());
      StructuredResponseCreateParams<NoteContentCompletion> params = paramsCaptor.getValue();
      String input = params.rawParams().input().flatMap(i -> i.text()).orElse("");
      assertFalse(input.contains("Previous note content (in JSON format):"));
      assertFalse(params.rawParams().instructions().orElse("").contains("Additional instruction"));
    }
  }
}
