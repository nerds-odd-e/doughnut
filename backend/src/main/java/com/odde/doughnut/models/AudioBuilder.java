package com.odde.doughnut.models;

import com.odde.doughnut.algorithms.AudioUtils;
import com.odde.doughnut.entities.*;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.multipart.MultipartFile;

public class AudioBuilder {

  private final AudioUtils audioUtils = new AudioUtils();

  public AudioBuilder() {}

  public Audio buildAudioFromAttachAudio(User user, @NotNull MultipartFile file)
      throws IOException {
    Audio audio = new Audio();
    audio.setUser(user);
    audio.setStorageType("db");
    audio.setName(file.getOriginalFilename());
    audio.setType(file.getContentType());
    AudioBlob audioBlob = getAudioBlob(file);
    audio.setAudioBlob(audioBlob);
    return audio;
  }

  AudioBlob getAudioBlob(MultipartFile file) throws IOException {
    AudioBlob audioBlob = new AudioBlob();
    audioBlob.setData(audioUtils.readAudioFile(file));
    return audioBlob;
  }
}
