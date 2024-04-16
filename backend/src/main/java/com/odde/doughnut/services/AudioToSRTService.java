package com.odde.doughnut.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AudioToSRTService {

  public String convertAudioToSRT(MultipartFile audioFile) {
    return "Mock audio content";
  }

}
