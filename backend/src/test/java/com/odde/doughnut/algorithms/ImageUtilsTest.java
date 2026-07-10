package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.InputStreamSource;

class ImageUtilsTest {

  @ParameterizedTest
  @CsvSource({
    "50, 50, 100, 100, 50, 50",
    "101, 2, 100, 100, 100, 1",
    "2, 101, 100, 100, 1, 100",
    "2001, 2, 2000, 2000, 2000, 1",
    "2, 2001, 2000, 2000, 1, 2000",
  })
  void scaledDimensions(
      int width, int height, int maxWidth, int maxHeight, int expectedWidth, int expectedHeight) {
    assertThat(
        ImageUtils.scaledDimensions(width, height, maxWidth, maxHeight),
        equalTo(new int[] {expectedWidth, expectedHeight}));
  }

  @Test
  void resizesOversizedImages() throws IOException {
    BufferedImage output = resized(buildImage(101, 2), "img.jpg", new ImageUtils(100, 100));
    assertThat(output.getWidth(), equalTo(100));
    assertThat(output.getHeight(), equalTo(1));
  }

  private BufferedImage resized(
      InputStreamSource stream, String originalFilename, ImageUtils imageUtils) throws IOException {
    byte[] bytes = imageUtils.toResizedImageByteArray(stream, originalFilename);
    return ImageIO.read(new ByteArrayInputStream(bytes));
  }

  private InputStreamSource buildImage(int width, int height) throws IOException {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    ImageIO.write(image, "jpg", stream);
    byte[] bytes = stream.toByteArray();
    return () -> new ByteArrayInputStream(bytes);
  }
}
