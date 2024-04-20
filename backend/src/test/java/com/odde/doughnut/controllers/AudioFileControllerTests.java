package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.dto.AudioUploadDTO;
import com.odde.doughnut.entities.Audio;
import com.odde.doughnut.entities.repositories.AudioBlobRepository;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
class AudioFileControllerTests {

  @Autowired AudioBlobRepository audioBlobRepository;

  @Mock RestTemplate restTemplate;

  @Autowired MakeMe makeMe;
  AudioFileController controller;

  @BeforeEach
  void setup() {
    controller = new AudioFileController(audioBlobRepository, restTemplate);
  }

  @Test
  void getContent() {
    Audio audio = makeMe.anAudio().please();
    makeMe.refresh(audio);
    assertNotNull(audio);
    ResponseEntity<byte[]> resp = controller.downloadAudio(audio);
    assertThat(resp.getStatusCode(), equalTo(HttpStatus.OK));
    assertThat(resp.getHeaders().getContentType().toString(), equalTo("audio/mp3"));
    assertThat(
        resp.getHeaders().getContentDisposition().toString(),
        equalTo("attachment; filename=\"example.mp3\""));
  }

  @Test
  void convertAudioToSRT() {
    MockMultipartFile mockFile =
        new MockMultipartFile("file", "test.mp3", "text/plain", "test".getBytes());
    var dto = new AudioUploadDTO();
    dto.setUploadAudioFile(mockFile);
    // Mocking the response entity
    ResponseEntity<String> mockResponseEntity = new ResponseEntity<>("test", HttpStatus.OK);
    when(restTemplate.exchange(
            any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
        .thenReturn(mockResponseEntity);

    ResponseEntity<String> resp = controller.uploadAudio(dto);

    assertThat(resp.getStatusCode(), equalTo(HttpStatus.OK));
    assertThat(resp.getBody(), equalTo("test"));
  }
}
