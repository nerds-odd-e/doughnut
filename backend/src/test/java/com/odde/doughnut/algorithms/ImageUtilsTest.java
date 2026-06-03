package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.InputStreamSource;

class ImageUtilsTest {

  @ParameterizedTest
  @CsvSource({
    "300, 300, 300, 300",
    "2001, 2, 2000, 1",
    "2, 2001, 1, 2000",
  })
  void resizeImageDimensions(int width, int height, int expectedWidth, int expectedHeight)
      throws IOException {
    BufferedImage output = resizeImage(buildImage(width, height), "img.jpg");
    assertThat(output.getWidth(), equalTo(expectedWidth));
    assertThat(output.getHeight(), equalTo(expectedHeight));
  }

  private BufferedImage resizeImage(InputStreamSource stream, String originalFilename)
      throws IOException {
    byte[] bytes = new ImageUtils().toResizedImageByteArray(stream, originalFilename);
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
