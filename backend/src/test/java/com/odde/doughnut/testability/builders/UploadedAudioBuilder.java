package com.odde.doughnut.testability.builders;

import java.io.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
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
      public @NotNull InputStream getInputStream() throws IOException {
        // Replace this byte array with your actual MP3 data
        byte[] mp3Data =
            new byte[] {
              // Your MP3 data here...
              // For example, you can add some placeholder bytes:
              (byte) 0xFF, (byte) 0xF3, (byte) 0x44, (byte) 0x1A, // Example bytes
              // Add more bytes as needed to make it non-empty
            };

        // Create a ByteArrayInputStream from the byte array
        return new ByteArrayInputStream(mp3Data);
      }
    };
  }

  @Contract(value = " -> new", pure = true)
  private @NotNull ByteArrayOutputStream buildAudio() throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    byte[] buffer = new byte[100];
    int bytesRead;
    try (InputStream inputStream = toInputSteamSource().getInputStream()) {
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
      }
    }
    return outputStream;
  }
}
