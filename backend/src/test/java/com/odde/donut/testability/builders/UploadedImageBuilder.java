package com.odde.donut.testability.builders;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

public class UploadedImageBuilder {
  private String name = "file";
  private String originalFilename = "my.png";
  private String contentType = "image/png";
  private byte[] bytes;

  public MultipartFile toMultiplePartFilePlease() {
    try {
      return new MockMultipartFile(
          name, originalFilename, contentType, bytes != null ? bytes : buildImage().toByteArray());
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException("not likely to happen");
    }
  }

  public UploadedImageBuilder originalFilename(String originalFilename) {
    this.originalFilename = originalFilename;
    return this;
  }

  public UploadedImageBuilder contentType(String contentType) {
    this.contentType = contentType;
    return this;
  }

  public UploadedImageBuilder bytes(byte[] bytes) {
    this.bytes = bytes;
    return this;
  }

  private ByteArrayOutputStream buildImage() throws IOException {
    BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2 = image.createGraphics();
    g2.drawString("Welcome to img", 0, 0);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    ImageIO.write(image, "png", stream);
    return stream;
  }
}
