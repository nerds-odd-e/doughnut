package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.testability.MakeMeWithoutDB;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamSource;

class ImageUtilsTest {
  MakeMeWithoutDB makeMe = new MakeMeWithoutDB();

  @Test
  void shouldNotTouchSmallImage() throws IOException {
    InputStreamSource stream = buildImage(300, 300);
    BufferedImage output = resizeImage(stream, "img.png");
    assertThat(output.getWidth(), equalTo(300));
    assertThat(output.getHeight(), equalTo(300));
  }

  @Test
  void shouldResizeLargeImage() throws IOException {
    InputStreamSource stream = buildImage(3000, 200);
    BufferedImage output = resizeImage(stream, "img.png");
    assertThat(output.getWidth(), equalTo(1000));
    assertThat(output.getHeight(), equalTo(66));
  }

  @Test
  void shouldResizeTooTallImage() throws IOException {
    InputStreamSource stream = buildImage(200, 3000);
    BufferedImage output = resizeImage(stream, "img.png");
    assertThat(output.getWidth(), equalTo(66));
    assertThat(output.getHeight(), equalTo(1000));
  }

  private BufferedImage resizeImage(InputStreamSource stream, String originalFilename)
      throws IOException {
    byte[] bytes = new ImageUtils().toResizedImageByteArray(stream, originalFilename);
    BufferedImage output = ImageIO.read(new ByteArrayInputStream(bytes));
    return output;
  }

  private InputStreamSource buildImage(int width, int height) throws IOException {
    return makeMe.anUploadedPicture().metrics(width, height).toInputSteamSource();
  }
}
