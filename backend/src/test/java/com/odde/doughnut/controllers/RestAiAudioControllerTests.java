package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestAiAudioControllerTests {
  @Autowired MakeMe makeMe;
  @Mock RestTemplate restTemplate;
  private UserModel userModel;
  RestAiAudioController controller;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();

    controller = new RestAiAudioController(restTemplate);
  }

  @Nested
  class ConvertAudioToSRT {
    Note note;
    AudioUploadDTO audioUploadDTO = new AudioUploadDTO();

    @BeforeEach
    void setup() {
      note = makeMe.aNote("new").creatorAndOwner(userModel).please();
    }

    @ParameterizedTest
    @ValueSource(strings = {"podcast.mp3", "podcast.m4a", "podcast.wav"})
    void convertingFormat(String filename) throws Exception {
      audioUploadDTO.setUploadAudioFile(
          new MockMultipartFile(filename, filename, "audio/mp3", new byte[] {}));
      ResponseEntity<String> mockResponseEntity = new ResponseEntity<>("test", HttpStatus.OK);
      when(restTemplate.exchange(
              any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
          .thenReturn(mockResponseEntity);
      String result = controller.convertSrt(audioUploadDTO).getBody();
      assertEquals("test", result);
    }

    @Test
    void convertAudioToSRT() throws IOException {
      MockMultipartFile mockFile =
          new MockMultipartFile("file", "test.mp3", "text/plain", "test".getBytes());
      var dto = new AudioUploadDTO();
      dto.setUploadAudioFile(mockFile);
      // Mocking the response entity
      ResponseEntity<String> mockResponseEntity = new ResponseEntity<>("test", HttpStatus.OK);
      when(restTemplate.exchange(
              any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
          .thenReturn(mockResponseEntity);

      String resp = controller.convertSrt(dto).getBody();

      assertThat(resp, equalTo("test"));
    }
  }
}
