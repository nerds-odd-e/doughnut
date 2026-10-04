package com.odde.donut.services.ai;

import com.odde.donut.services.openAiApis.OpenAiApiHandler;
import java.io.IOException;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public final class OtherAiServices {
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

  public String getTranscriptionFromAudio(String filename, byte[] bytes) throws IOException {
    return openAiApiHandler.getTranscription(filename, bytes);
  }
}
