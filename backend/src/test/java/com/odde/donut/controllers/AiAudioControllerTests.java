package com.odde.donut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odde.donut.controllers.dto.AudioUploadDTO;
import com.odde.donut.services.ai.NoteContentCompletion;
import com.odde.donut.services.ai.TextFromAudioWithCallInfo;
import com.odde.donut.testability.OpenAiStructuredResponseMock;
import com.openai.client.OpenAIClient;
import com.openai.models.audio.transcriptions.Transcription;
import com.openai.models.audio.transcriptions.TranscriptionCreateParams;
import com.openai.models.audio.transcriptions.TranscriptionCreateResponse;
import com.openai.models.responses.ResponseTextConfig;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.openai.services.blocking.AudioService;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class AiAudioControllerTests extends ControllerTestBase {
  @Autowired AiAudioController controller;

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  OpenAiStructuredResponseMock openAiStructuredResponseMock;

  @BeforeEach
  void commonSetup() {
    openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);
    openAiStructuredResponseMock.stubStructuredResponse(new NoteContentCompletion("test123"));
    mockTranscriptionSrtResponse("test transcription");
  }

  private void mockTranscriptionSrtResponse(String responseBody) {
    var audioService = Mockito.mock(AudioService.class, Mockito.RETURNS_DEEP_STUBS);
    when(officialClient.audio()).thenReturn(audioService);
    var transcriptionResponse =
        TranscriptionCreateResponse.ofTranscription(
            Transcription.builder().text(responseBody).build());
    when(audioService.transcriptions().create(any(TranscriptionCreateParams.class)))
        .thenReturn(transcriptionResponse);
  }

  private AudioUploadDTO audioUpload(String filename) {
    var dto = new AudioUploadDTO();
    dto.setUploadAudioFile(
        new MockMultipartFile(filename, filename, "audio/mp3", "test".getBytes()));
    return dto;
  }

  @Nested
  class ConvertAudioToTextTests {
    private AudioUploadDTO audioUploadDTO;

    @BeforeEach
    void setup() {
      audioUploadDTO = audioUpload("test.mp3");
    }

    @ParameterizedTest
    @ValueSource(strings = {"podcast.mp3", "podcast.m4a", "podcast.wav"})
    void convertingFormat(String filename) throws Exception {
      NoteContentCompletion result =
          controller
              .audioToText(audioUpload(filename))
              .map(TextFromAudioWithCallInfo::getCompletionFromAudio)
              .orElseThrow();
      assertThat(result.content).isEqualTo("test123");
    }

    @Test
    void shouldIncludeAdditionalInstructions() throws IOException {
      audioUploadDTO.setAdditionalProcessingInstructions("Translate to Spanish");

      controller.audioToText(audioUploadDTO);

      StructuredResponseCreateParams<NoteContentCompletion> params = captureCompletionParams();
      assertThat(params.rawParams().instructions().orElse(""))
          .contains("Additional instruction:\nTranslate to Spanish");
      assertThat(params.rawParams().text().flatMap(ResponseTextConfig::format)).isPresent();
    }

    @Test
    void shouldIncludePreviousContentAsUserMessage() throws IOException {
      audioUploadDTO.setPreviousNoteContentToAppendTo("Previous text with trailing space ");

      controller.audioToText(audioUploadDTO);

      String input =
          captureCompletionParams().rawParams().input().flatMap(i -> i.text()).orElse("");
      assertThat(input)
          .isEqualTo(
              "Previous note content (in JSON format):\n"
                  + "{\"previousNoteContentToAppendTo\": \"Previous text with trailing space \"}");
    }

    @Test
    void shouldWorkWithoutPreviousContent() throws IOException {
      controller.audioToText(audioUploadDTO);

      String input =
          captureCompletionParams().rawParams().input().flatMap(i -> i.text()).orElse("");
      assertThat(input).doesNotContain("Previous note content (in JSON format):");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private StructuredResponseCreateParams<NoteContentCompletion> captureCompletionParams() {
      ArgumentCaptor<StructuredResponseCreateParams<NoteContentCompletion>> paramsCaptor =
          ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
      verify(openAiStructuredResponseMock.responseService()).create(paramsCaptor.capture());
      return paramsCaptor.getValue();
    }
  }
}
