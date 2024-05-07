package com.odde.doughnut.validators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.controllers.dto.AudioUploadDTO;
import jakarta.validation.*;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockMultipartFile;

public class UploadAudioDtoValidatorTest {

  private Validator validator;
  private final AudioUploadDTO audioUploadDTO = new AudioUploadDTO();

  @BeforeEach
  public void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  public void defaultNoteFromMakeMeIsValidate() {
    assertThat(getViolations(), is(empty()));
  }

  @ParameterizedTest
  @CsvSource({"podcast.mp3, audio/mpeg", "podcast.m4a, audio/mp4", "podcast.wav, audio/wav"})
  void shouldSucceedOnValidAudioFileFormat(String filename, String contentType) {
    audioUploadDTO.setUploadAudioFile(
        new MockMultipartFile(filename, filename, contentType, new byte[] {1}));
    assertThat(getViolations(), is(empty()));
  }

  @ParameterizedTest
  @CsvSource({"something.txt, text", "youtube.avi, video/x-msvideo"})
  void shouldFailOnInvalidAudioFileFormat(String filename, String contentType) {
    audioUploadDTO.setUploadAudioFile(
        new MockMultipartFile(filename, filename, contentType, new byte[] {1}));
    assertThat(getViolations(), is(not(empty())));
    Path propertyPath = getViolations().stream().findFirst().get().getPropertyPath();
    assertThat(propertyPath.toString(), equalTo("uploadAudioFile"));
    String message = getViolations().stream().findFirst().get().getMessage();
    assertThat(message, containsString("Invalid file"));
  }

  @Test
  void shouldFailOnFileSize() {
    String filename = "big_file.mp3";
    byte[] bytes = new byte[1024 * 1024 * 20 + 1];
    audioUploadDTO.setUploadAudioFile(
        new MockMultipartFile(filename, filename, "audio/mpeg", bytes));
    Path propertyPath = getViolations().stream().findFirst().get().getPropertyPath();
    assertThat(propertyPath.toString(), equalTo("uploadAudioFile"));
  }

  private Set<ConstraintViolation<AudioUploadDTO>> getViolations() {
    return validator.validate(audioUploadDTO);
  }
}
