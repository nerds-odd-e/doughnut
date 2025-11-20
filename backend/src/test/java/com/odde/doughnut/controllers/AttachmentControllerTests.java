package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;

import com.odde.doughnut.entities.Image;
import com.odde.doughnut.testability.MakeMe;
import org.hamcrest.Matchers;
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
class AttachmentControllerTests {
  @Autowired MakeMe makeMe;
  @Autowired AttachmentController controller;

  @Test
  void imageDownload() {
    Image image = makeMe.anImage().please();
    ResponseEntity<byte[]> resp = controller.showImage(image, "filename");
    assertThat(resp.getStatusCode(), Matchers.equalTo(HttpStatus.OK));
    assertThat(resp.getHeaders().getContentType().toString(), Matchers.equalTo("image/png"));
    assertThat(
        resp.getHeaders().getContentDisposition().toString(),
        Matchers.equalTo("inline; filename=\"example.png\""));
  }
}
