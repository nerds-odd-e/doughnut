package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.odde.doughnut.entities.Audio;
import com.odde.doughnut.entities.repositories.AudioBlobRepository;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AudioFileControllerTests {

  @Autowired AudioBlobRepository audioBlobRepository;

  @Autowired MakeMe makeMe;
  AudioFileController controller;

  @BeforeEach
  void setup() {
    controller = new AudioFileController(audioBlobRepository);
  }

  @Test
  void getContent() {
    Audio audio = makeMe.anAudio().please();
    makeMe.refresh(audio);
    assertNotNull(audio);
    ResponseEntity<byte[]> resp = controller.show(audio, "filename");
    assertThat(resp.getStatusCode(), equalTo(HttpStatus.OK));
    assertThat(resp.getHeaders().getContentType().toString(), equalTo("audio/mp3"));
    assertThat(
        resp.getHeaders().getContentDisposition().toString(),
        equalTo("inline; filename=\"example.mp3\""));
  }

  @Test
  void convertAudioToSRT() {
    MockMultipartFile mockFile =
        new MockMultipartFile("file", "test.mp4", "text/plain", "test".getBytes());
    ResponseEntity<String> resp = controller.convertAudioToSRT(mockFile);
    assertThat(resp.getStatusCode(), equalTo(HttpStatus.OK));
    assertThat(resp.getBody(), equalTo("test"));
  }
}
