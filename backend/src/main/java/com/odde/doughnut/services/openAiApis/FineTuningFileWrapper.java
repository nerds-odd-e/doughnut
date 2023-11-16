package com.odde.doughnut.services.openAiApis;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.springframework.http.HttpStatus;

record FineTuningFileWrapper(List<?> examples, String subFileName) {
  public <T> T withFileToBeUploaded(Function<MultipartBody.Part, T> consumer) throws IOException {
    String jsonString = toJsonL();

    File tempFile = File.createTempFile(subFileName, ".jsonl");
    try {
      Files.write(tempFile.toPath(), jsonString.getBytes(), StandardOpenOption.WRITE);

      RequestBody fileRequestBody =
          RequestBody.create(tempFile, MediaType.parse("application/octet-stream"));
      MultipartBody.Part filePart =
          MultipartBody.Part.createFormData("file", tempFile.getName(), fileRequestBody);
      return consumer.apply(filePart);
    } finally {
      tempFile.delete();
    }
  }

  private String toJsonL() {
    if (examples.size() < 10) {
      throw new OpenAIServiceErrorException(
          "Positive feedback cannot be less than 10.", HttpStatus.BAD_REQUEST);
    }
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return examples.stream()
        .map(
            x -> {
              try {
                return objectMapper.writeValueAsString(x);
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            })
        .collect(Collectors.joining("\n"));
  }
}
