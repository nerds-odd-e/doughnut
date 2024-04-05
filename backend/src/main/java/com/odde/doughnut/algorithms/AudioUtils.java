package com.odde.doughnut.algorithms;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.InputStreamSource;

public class AudioUtils {
  public byte @NotNull [] readAudioFile(InputStreamSource inputStreamSource) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    byte[] buffer = new byte[4096];
    int bytesRead;

    try (var inputStream = inputStreamSource.getInputStream()) {
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
      }
    }

    return outputStream.toByteArray();
  }
}
