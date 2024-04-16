package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.odde.doughnut.entities.Audio;
import com.odde.doughnut.entities.repositories.AudioBlobRepository;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
    //   makeMe.refresh(image);
    //   ResponseEntity<byte[]> resp = controller.show(image, "filename");
    //   assertThat(resp.getStatusCode(), equalTo(HttpStatus.OK));
    //   assertThat(resp.getHeaders().getContentType().toString(), equalTo("image/png"));
    //   assertThat(
    //       resp.getHeaders().getContentDisposition().toString(),
    //       equalTo("inline; filename=\"example.png\""));
    assertNotNull(audio);
  }
}
