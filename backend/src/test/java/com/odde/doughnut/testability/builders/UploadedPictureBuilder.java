package com.odde.doughnut.testability.builders;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

public class UploadedPictureBuilder {
  private int width = 2;
  private int height = 2;
  private String name = "file";
  private String originalFilename = "my.png";
  private String contentType = "image/png";

  public MultipartFile toMultiplePartFilePlease() {
    try {
      return new MockMultipartFile(name, originalFilename, contentType, buildImage().toByteArray());
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException("not likely to happen");
    }
  }

  public InputStreamSource toInputSteamSource() {
    return new InputStreamSource() {
      @Override
      public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(buildImage().toByteArray());
      }
    };
  }

  public UploadedPictureBuilder metrics(int width, int height) {
    this.width = width;
    this.height = height;
    return this;
  }

  private ByteArrayOutputStream buildImage() throws IOException {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2 = image.createGraphics();
    g2.drawString("Welcome to img", 0, 0);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    ImageIO.write(image, "png", stream);
    return stream;
  }
}
