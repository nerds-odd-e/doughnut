package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Audio;
import com.odde.doughnut.entities.repositories.AudioBlobRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/audio")
public class AudioFileController {
  private final AudioBlobRepository audioBlobRepository;

  public AudioFileController(AudioBlobRepository audioBlobRepository) {
    this.audioBlobRepository = audioBlobRepository;
  }

  @GetMapping("/{audio}/{fileName}")
  public ResponseEntity<byte[]> show(
      @PathVariable("audio") @Schema(type = "integer") Audio audio,
      @PathVariable("fileName") String filename) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + audio.getName() + "\"")
        .header(HttpHeaders.CONTENT_TYPE, audio.getType())
        .body(audioBlobRepository.findById(audio.getAudioBlobId()).get().getData());
  }

  @PostMapping()
  public ResponseEntity<String> upload(@RequestBody MultipartFile audioFile,
                                       @PathVariable("convert") Boolean toConvert) {
    return ResponseEntity.ok("test");
  }
}
