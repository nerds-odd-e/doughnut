package com.odde.doughnut.testability.builders;

import java.awt.*;
import java.io.*;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

public class UploadedAudioBuilder {
  private String name = "file";
  private String originalFilename = "my.mp3";
  private String contentType = "audio/mp3";

  public MultipartFile toMultiplePartFilePlease() {
    try {
      return new MockMultipartFile(name, originalFilename, contentType, buildAudio().toByteArray());
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException("not likely to happen");
    }
  }

  public InputStreamSource toInputSteamSource() {
    return new InputStreamSource() {
      @Override
      public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(buildAudio().toByteArray());
      }
    };
  }

  private ByteArrayOutputStream buildAudio() throws IOException {
    return new ByteArrayOutputStream();
  }
}
