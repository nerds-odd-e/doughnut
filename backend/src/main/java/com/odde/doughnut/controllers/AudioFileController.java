package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.AudioUploadDTO;
import com.odde.doughnut.entities.Audio;
import com.odde.doughnut.entities.repositories.AudioBlobRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/audio")
public class AudioFileController {
  private final AudioBlobRepository audioBlobRepository;

  @Value("${spring.openai.token}")
  private String openAiToken;

  private RestTemplate restTemplate;

  public AudioFileController(AudioBlobRepository audioBlobRepository, RestTemplate restTemplate) {
    this.audioBlobRepository = audioBlobRepository;
    this.restTemplate = restTemplate;
  }

  @GetMapping("/{audio}")
  public ResponseEntity<byte[]> downloadAudio(
      @PathVariable("audio") @Schema(type = "integer") Audio audio) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + audio.getName() + "\"")
        .header(HttpHeaders.CONTENT_TYPE, audio.getType())
        .body(audioBlobRepository.findById(audio.getAudioBlobId()).get().getData());
  }

  @PostMapping(value = "/{convert}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<String> upload(
      @Valid @ModelAttribute AudioUploadDTO audioFile, @PathVariable("convert") Boolean toConvert) {
    if (toConvert) {
      var url = "https://api.openai.com/v1/audio/transcriptions";
      var filename = audioFile.getUploadAudioFile().getOriginalFilename();

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.MULTIPART_FORM_DATA);
      headers.setBearerAuth(openAiToken);

      MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();
      ContentDisposition contentDisposition =
          ContentDisposition.builder("form-data").name("file").filename(filename).build();

      fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
      HttpEntity<byte[]> fileEntity;
      try {
        fileEntity = new HttpEntity<>(audioFile.getUploadAudioFile().getBytes(), fileMap);
      } catch (IOException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Error reading audio file");
      }

      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      body.add("file", fileEntity);
      body.add("model", "whisper-1");
      body.add("response_format", "srt");

      HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

      return restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
    }
    return ResponseEntity.ok("Successfully uploaded audio file");
  }
}
