package com.odde.donut.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openai.models.audio.transcriptions.Transcription;
import com.openai.models.audio.transcriptions.TranscriptionCreateParams;
import com.openai.models.audio.transcriptions.TranscriptionCreateResponse;
import com.openai.services.blocking.AudioService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

class AiAudioControllerTests extends ControllerTestBase {
  @Autowired MockMvc mockMvc;

  private static final String THREE_SEGMENTS =
      "1\n00:00:00,000 --> 00:00:03,000\nThe orchard has apple trees.\n\n"
          + "2\n00:00:03,000 --> 00:00:06,000\nThese facts are finished.\n\n"
          + "3\n00:00:06,000 --> 00:00:09,000\nThe book that I";

  private void mockTranscriptionSrtResponse(String responseBody) {
    var audioService = Mockito.mock(AudioService.class, Mockito.RETURNS_DEEP_STUBS);
    when(officialClient.audio()).thenReturn(audioService);
    var transcriptionResponse =
        TranscriptionCreateResponse.ofTranscription(
            Transcription.builder().text(responseBody).build());
    when(audioService.transcriptions().create(any(TranscriptionCreateParams.class)))
        .thenReturn(transcriptionResponse);
  }

  private ResultActions upload(String filename, boolean midSpeech) throws Exception {
    return mockMvc
        .perform(
            multipart("/api/audio/audio-to-text")
                .file(
                    new MockMultipartFile(
                        "uploadAudioFile", filename, "audio/mpeg", "test".getBytes()))
                .param("midSpeech", String.valueOf(midSpeech)))
        .andExpect(status().isOk());
  }

  @Nested
  class ConvertAudioToTextTests {

    @ParameterizedTest
    @ValueSource(strings = {"podcast.mp3", "podcast.m4a", "podcast.wav"})
    void convertingFormat(String filename) throws Exception {
      mockTranscriptionSrtResponse("1\n00:00:00,000 --> 00:00:03,000\ntest transcription");

      upload(filename, false).andExpect(jsonPath("$.dictatedText").value("test transcription"));
    }

    @Test
    void midSpeechWritesTheTranscriptionOfAllButTheLastSegment() throws Exception {
      mockTranscriptionSrtResponse(THREE_SEGMENTS);

      upload("test.mp3", true)
          .andExpect(
              jsonPath("$.dictatedText")
                  .value("The orchard has apple trees. These facts are finished."))
          .andExpect(jsonPath("$.endTimestamp").value("00:00:06,000"));
    }

    @Test
    void stopWritesTheTranscriptionOfEverySegment() throws Exception {
      mockTranscriptionSrtResponse(THREE_SEGMENTS);

      upload("test.mp3", false)
          .andExpect(
              jsonPath("$.dictatedText")
                  .value("The orchard has apple trees. These facts are finished. The book that I"))
          .andExpect(jsonPath("$.endTimestamp").value("00:00:09,000"));
    }

    @Test
    void shouldHoldBackSingleSegmentOfMidSpeechUpload() throws Exception {
      mockTranscriptionSrtResponse("1\n00:00:00,000 --> 00:00:03,000\nunfinished sentence\n\n\n");

      upload("test.mp3", true)
          .andExpect(jsonPath("$.dictatedText").value(""))
          .andExpect(jsonPath("$.endTimestamp").value("00:00:00,000"));
    }
  }
}
