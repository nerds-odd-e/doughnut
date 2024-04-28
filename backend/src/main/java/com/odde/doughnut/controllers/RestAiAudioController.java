package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Audio;
import com.odde.doughnut.entities.Note;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/notes")
class RestAiAudioController {

  @Value("${spring.openai.token}")
  private String openAiToken;

  private final RestTemplate restTemplate;

  public RestAiAudioController(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @PatchMapping(path = "/{note}/audio-to-srt")
  @Transactional
  public ResponseEntity<String> convertAudioToSRT(
      @PathVariable(name = "note") @Schema(type = "integer") Note note) {
    Audio audio = note.getNoteAccessories().getUploadAudio();
    return audioToSrt(audio.getName(), audio.getBlob().getData());
  }

  @PostMapping(
      path = "/convertSrt",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  @Transactional
  public ResponseEntity<String> convertSrt(@Valid @ModelAttribute AudioUploadDTO audioFile)
      throws IOException {

    return audioToSrt(
        audioFile.getUploadAudioFile().getOriginalFilename(),
        audioFile.getUploadAudioFile().getBytes());
  }

  private ResponseEntity<String> audioToSrt(String filename, byte[] bytes) {
    var url = "https://api.openai.com/v1/audio/transcriptions";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.setBearerAuth(openAiToken);

    MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();
    ContentDisposition contentDisposition =
        ContentDisposition.builder("form-data").name("file").filename(filename).build();

    fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", new HttpEntity<byte[]>(bytes, fileMap));
    body.add("model", "whisper-1");
    body.add("response_format", "srt");

    HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

    return restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
  }
}
