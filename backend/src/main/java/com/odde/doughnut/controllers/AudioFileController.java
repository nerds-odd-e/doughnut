package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Audio;
import com.odde.doughnut.entities.repositories.AudioBlobRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/audio")
public class AudioFileController {
  private final AudioBlobRepository audioBlobRepository;

  @Value("${spring.openai.token}")
  private String openAiToken;

  private RestTemplate restTemplate;

  public AudioFileController(AudioBlobRepository audioBlobRepository, RestTemplate restTemplate) {
    this.audioBlobRepository = audioBlobRepository;
    this.restTemplate = restTemplate;
  }

  @GetMapping("/{audio}/{fileName}")
  public ResponseEntity<byte[]> downloadAudio(
      @PathVariable("audio") @Schema(type = "integer") Audio audio,
      @PathVariable("fileName") String filename) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + audio.getName() + "\"")
        .header(HttpHeaders.CONTENT_TYPE, audio.getType())
        .body(audioBlobRepository.findById(audio.getAudioBlobId()).get().getData());
  }

  @PostMapping("/{convert}")
  public ResponseEntity<String> upload(
      @RequestBody MultipartFile audioFile, @PathVariable("convert") Boolean toConvert) {
    if (toConvert) {
      var url = "https://api.openai.com/v1/audio/transcriptions";
      var filename = audioFile.getOriginalFilename();

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.MULTIPART_FORM_DATA);
      headers.setBearerAuth(openAiToken);

      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      assert filename != null;
      body.add("file", new FileSystemResource(filename));
      body.add("model", "whisper-1");

      HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

      ResponseEntity<String> response =
          restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
    }
    return ResponseEntity.ok("test");
  }
}
