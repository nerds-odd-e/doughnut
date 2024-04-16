package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.repositories.AudioBlobRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/audio")
public class AudioFileController {
  private final AudioBlobRepository audioBlobRepository;

  public AudioFileController(AudioBlobRepository audioBlobRepository) {
    this.audioBlobRepository = audioBlobRepository;
  }

  @GetMapping("/audio")
  public ResponseEntity<byte[]> show() {
    //     @PathVariable("image") @Schema(type = "integer") Image image,
    //     @PathVariable("fileName") String filename) {
    //   return ResponseEntity.ok()
    //       .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getName() +
    // "\"")
    //       .header(HttpHeaders.CONTENT_TYPE, image.getType())
    //       .body(imageBlobRepository.findById(image.getImageBlobId()).get().getData());
    return null;
  }
}
