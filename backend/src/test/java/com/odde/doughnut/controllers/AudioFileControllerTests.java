package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import com.odde.doughnut.entities.Audio;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AudioFileControllerTests {
  @Autowired MakeMe makeMe;
  AudioFileController controller;

  @BeforeEach
  void setup() {
    controller = new AudioFileController();
  }

  @Test
  void getContent() {
    Audio audio = makeMe.anAudio().please();
    ResponseEntity<byte[]> resp = controller.downloadAudio(audio);
    assertThat(resp.getStatusCode(), equalTo(HttpStatus.OK));
    assertThat(resp.getHeaders().getContentType().toString(), equalTo("audio/mp3"));
    assertThat(
        resp.getHeaders().getContentDisposition().toString(),
        equalTo("attachment; filename=\"example.mp3\""));
  }
}
