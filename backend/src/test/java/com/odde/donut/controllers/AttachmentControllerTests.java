package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Image;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AttachmentControllerTests extends ControllerTestBase {
  @Autowired AttachmentController controller;

  @Test
  void imageDownload() {
    Image image = makeMe.anImage().please();
    ResponseEntity<byte[]> resp = controller.showImage(image, "filename");
    assertThat(resp.getStatusCode(), equalTo(HttpStatus.OK));
    assertThat(resp.getHeaders().getContentType().toString(), equalTo("image/png"));
    assertThat(
        resp.getHeaders().getContentDisposition().toString(),
        equalTo("inline; filename=\"example.png\""));
  }
}
