package com.odde.donut.services.ai;

import com.odde.donut.services.openAiApis.OpenAiApiHandler;
import com.openai.models.audio.AudioResponseFormat;
import java.io.IOException;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public final class OtherAiServices {
  private static final String TITLE_TRANSCRIPTION_PROMPT =
      "Transcribe the spoken words as a note title or title fragment. "
          + "Use concise, faithful wording; preserve the speaker's meaning and every substantive detail. "
          + "Omit an ordinary sentence-ending period, but retain appropriate punctuation, "
          + "including a question mark for a question. "
          + "Return only the dictated title fragment; do not invent or rewrite surrounding title text.";

  private final OpenAiApiHandler openAiApiHandler;

  public OtherAiServices(OpenAiApiHandler openAiApiHandler) {
    this.openAiApiHandler = openAiApiHandler;
  }

  public List<String> getAvailableGptModels() {
    List<String> modelVersionOptions = new ArrayList<>();

    openAiApiHandler
        .getModels()
        .forEach(
            (e) -> {
              if (e.id().startsWith("ft:") || e.id().startsWith("gpt")) {
                modelVersionOptions.add(e.id());
              }
            });

    return modelVersionOptions;
  }

  public String getSrtTranscription(String filename, byte[] bytes) throws IOException {
    return openAiApiHandler.getTranscription(
        filename, bytes, "whisper-1", AudioResponseFormat.SRT, Optional.empty());
  }

  public String getTextTranscription(String filename, byte[] bytes, boolean titleIntent)
      throws IOException {
    return openAiApiHandler.getTranscription(
        filename,
        bytes,
        "gpt-4o-mini-transcribe",
        AudioResponseFormat.TEXT,
        titleIntent ? Optional.of(TITLE_TRANSCRIPTION_PROMPT) : Optional.empty());
  }
}
