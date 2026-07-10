package com.odde.doughnut.algorithms;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.springframework.core.io.InputStreamSource;

public class ImageUtils {
  private final int maxWidth;
  private final int maxHeight;

  public ImageUtils() {
    this(2000, 2000);
  }

  ImageUtils(int maxWidth, int maxHeight) {
    this.maxWidth = maxWidth;
    this.maxHeight = maxHeight;
  }

  public byte[] toResizedImageByteArray(InputStreamSource file, String originalFilename)
      throws IOException {
    BufferedImage originalImage = ImageIO.read(file.getInputStream());
    int width = originalImage.getWidth();
    int height = originalImage.getHeight();

    if (width <= maxWidth && height <= maxHeight) {
      return file.getInputStream().readAllBytes();
    }

    int[] scaled = scaledDimensions(width, height, maxWidth, maxHeight);
    BufferedImage resizedImage = resizeImage(originalImage, scaled[0], scaled[1]);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(resizedImage, getFileExtension(originalFilename), baos);
    return baos.toByteArray();
  }

  static int[] scaledDimensions(int width, int height, int maxWidth, int maxHeight) {
    if (width > maxWidth) {
      height = height * maxWidth / width;
      width = maxWidth;
    }

    if (height > maxHeight) {
      width = width * maxHeight / height;
      height = maxHeight;
    }

    return new int[] {width, height};
  }

  private String getFileExtension(String name) {
    int lastIndexOf = name.lastIndexOf(".");
    if (lastIndexOf == -1) {
      return ""; // empty extension
    }
    return name.substring(lastIndexOf + 1);
  }

  private BufferedImage resizeImage(
      BufferedImage originalImage, int targetWidth, int targetHeight) {
    BufferedImage resizedImage =
        new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics2D = resizedImage.createGraphics();
    graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
    graphics2D.dispose();
    return resizedImage;
  }
}
