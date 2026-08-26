package com.odde.donut.validators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.controllers.dto.AudioUploadDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockMultipartFile;

class UploadAudioDtoValidatorTest {

  private Validator validator;
  private final AudioUploadDTO audioUploadDTO = new AudioUploadDTO();

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void emptyDtoIsValid() {
    assertThat(getViolations(), is(empty()));
  }

  @ParameterizedTest
  @CsvSource({"podcast.mp3, audio/mpeg", "podcast.m4a, audio/mp4", "podcast.wav, audio/wav"})
  void acceptsValidAudioFileFormat(String filename, String contentType) {
    audioUploadDTO.setUploadAudioFile(
        new MockMultipartFile(filename, filename, contentType, new byte[] {1}));
    assertThat(getViolations(), is(empty()));
  }

  @ParameterizedTest
  @CsvSource({"something.txt, text", "youtube.avi, video/x-msvideo"})
  void rejectsInvalidAudioFileFormat(String filename, String contentType) {
    audioUploadDTO.setUploadAudioFile(
        new MockMultipartFile(filename, filename, contentType, new byte[] {1}));
    ConstraintViolation<AudioUploadDTO> violation = getViolations().iterator().next();
    assertThat(violation.getPropertyPath().toString(), equalTo("uploadAudioFile"));
    assertThat(violation.getMessage(), containsString("Invalid file type"));
  }

  @Test
  void rejectsOversizedAudioFile() {
    audioUploadDTO.setUploadAudioFile(
        new MockMultipartFile(
            "big_file.mp3", "big_file.mp3", "audio/mpeg", new byte[1024 * 1024 * 20 + 1]));
    assertThat(getViolations().iterator().next().getMessage(), containsString("File size exceeds"));
  }

  private Set<ConstraintViolation<AudioUploadDTO>> getViolations() {
    return validator.validate(audioUploadDTO);
  }
}
