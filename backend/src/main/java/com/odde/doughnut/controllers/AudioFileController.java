package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Audio;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/attachments/audio")
public class AudioFileController {
  private final UserModel currentUser;

  public AudioFileController(UserModel currentUser) {
    this.currentUser = currentUser;
  }

  @GetMapping("/{audio}")
  public ResponseEntity<byte[]> downloadAudio(
      @PathVariable("audio") @Schema(type = "integer") Audio audio)
      throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(audio);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + audio.getName() + "\"")
        .header(HttpHeaders.CONTENT_TYPE, audio.getType())
        .body(audio.getAudioBlob().getData());
  }
}
