package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Audio;
import com.odde.doughnut.entities.repositories.AudioBlobRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audio")
public class AudioFileController {
  private final AudioBlobRepository audioBlobRepository;

  public AudioFileController(AudioBlobRepository audioBlobRepository) {
    this.audioBlobRepository = audioBlobRepository;
  }

  @GetMapping("/{audio}")
  public ResponseEntity<byte[]> downloadAudio(
      @PathVariable("audio") @Schema(type = "integer") Audio audio) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + audio.getName() + "\"")
        .header(HttpHeaders.CONTENT_TYPE, audio.getType())
        .body(audioBlobRepository.findById(audio.getAudioBlobId()).get().getData());
  }
}
