package com.odde.doughnut.controllers;

import com.odde.doughnut.services.AudioToSRTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest
@ActiveProfiles("test")
public class AudioToSRTControllerTest {

  AudioToSRTController controller;

  @BeforeEach
  void setup() {controller  = new AudioToSRTController(new AudioToSRTService());}

  @Test
  void convertAudioToSRT() {
  // Mock audio file
    MockMultipartFile audioFile = new MockMultipartFile(
      "file",
      "test-audio.mp3",
      "audio/mp3",
      "Mock audio content".getBytes(StandardCharsets.UTF_8)
    );
    String response = controller.convertAudioToSRT(audioFile);
    assertThat(response,equalTo("Mock audio content"));
  }

}
