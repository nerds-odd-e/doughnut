package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Audio;
import com.odde.doughnut.entities.Image;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import org.hamcrest.Matchers;
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
class AttachmentControllerTests {
  @Autowired MakeMe makeMe;
  AttachmentController controller;
  UserModel currentUser;

  @BeforeEach
  void setup() {
    currentUser = makeMe.aUser().toModelPlease();
    controller = new AttachmentController(currentUser);
  }

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

  @Test
  void unauthorizedAccess() {
    User anotherUser = makeMe.aUser().please();
    Audio audio = makeMe.anAudio().user(anotherUser).please();
    makeMe.refresh(audio);
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.downloadAudio(audio),
        "User does not have access to the audio");
  }

  @Test
  void getContent() throws UnexpectedNoAccessRightException {
    Audio audio = makeMe.anAudio().user(currentUser.getEntity()).please();
    ResponseEntity<byte[]> resp = controller.downloadAudio(audio);
    assertThat(resp.getStatusCode(), equalTo(HttpStatus.OK));
    assertThat(resp.getHeaders().getContentType().toString(), equalTo("audio/mp3"));
    assertThat(
        resp.getHeaders().getContentDisposition().toString(),
        equalTo("attachment; filename=\"example.mp3\""));
  }
}
