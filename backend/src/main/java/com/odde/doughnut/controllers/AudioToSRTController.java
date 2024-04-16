package com.odde.doughnut.controllers;

import com.odde.doughnut.services.AudioToSRTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AudioToSRTController {

  final AudioToSRTService service;

  @Autowired
  public AudioToSRTController(AudioToSRTService service) {
    this.service = service;
  }

  @PostMapping("convert-to-srt")
  public String convertAudioToSRT(@RequestParam("file") MultipartFile audioFile) {
    return service.convertAudioToSRT(audioFile);
  }

}
