package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ai.TextFromAudio;
import com.odde.doughnut.services.openAiApis.OpenAiApiExtended;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
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
class RestAiAudioControllerTests {
  @Autowired MakeMe makeMe;
  private UserModel userModel;
  RestAiAudioController controller;
  @Mock OpenAiApiExtended openAiApi;
  OpenAIChatCompletionMock openAIChatCompletionMock;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();

    controller = new RestAiAudioController(openAiApi, makeMe.modelFactoryService);
    TextFromAudio textFromAudio = new TextFromAudio();
    textFromAudio.setTextFromAudio("test123");
    openAIChatCompletionMock = new OpenAIChatCompletionMock(openAiApi);
    openAIChatCompletionMock.mockChatCompletionAndReturnToolCall(
        textFromAudio, "audio_transcription_to_text");
  }

  @Nested
  class ConvertAudioToSRT {
    AudioUploadDTO audioUploadDTO = new AudioUploadDTO();

    @BeforeEach
    void setup() {
      when(openAiApi.createTranscriptionSrt(any(RequestBody.class)))
          .thenReturn(Single.just(ResponseBody.create("test", null)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"podcast.mp3", "podcast.m4a", "podcast.wav"})
    void convertingFormat(String filename) throws Exception {
      audioUploadDTO.setUploadAudioFile(
          new MockMultipartFile(filename, filename, "audio/mp3", new byte[] {}));
      String result =
          controller.audioToText(audioUploadDTO).map(TextFromAudio::getTextFromAudio).orElse("");
      assertEquals("test123", result);
    }

    @Test
    void convertAudioToSRT() throws IOException {
      MockMultipartFile mockFile =
          new MockMultipartFile("file", "test.mp3", "text/plain", "test".getBytes());
      var dto = new AudioUploadDTO();
      dto.setUploadAudioFile(mockFile);
      String resp = controller.audioToText(dto).map(TextFromAudio::getTextFromAudio).orElse("");
      assertThat(resp, equalTo("test123"));
    }
  }
}
