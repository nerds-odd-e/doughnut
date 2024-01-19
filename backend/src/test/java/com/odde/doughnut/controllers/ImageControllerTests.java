package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.entities.Image;
import com.odde.doughnut.entities.repositories.ImageBlobRepository;
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
class ImageControllerTests {
  @Autowired ImageBlobRepository imageBlobRepository;

  @Autowired MakeMe makeMe;
  ImageController controller;

  @BeforeEach
  void setup() {
    controller = new ImageController(imageBlobRepository);
  }

  @Test
  void contentType() {
    Image image = makeMe.anImage().please();
    makeMe.refresh(image);
    ResponseEntity<byte[]> resp = controller.show(image, "filename");
    assertThat(resp.getStatusCode(), equalTo(HttpStatus.OK));
    assertThat(resp.getHeaders().getContentType().toString(), equalTo("image/png"));
    assertThat(
        resp.getHeaders().getContentDisposition().toString(),
        equalTo("inline; filename=\"example.png\""));
  }
}
