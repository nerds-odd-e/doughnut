package com.odde.doughnut.controllers.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.theokanning.openai.file.File;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class AiTrainingFile {

  /**
   * The unique id of this file.
   */
  String id;

  /**
   * The creation time in epoch seconds.
   */
  @JsonProperty("created_at")
  Long createdAt;

  /**
   * The name of the file.
   */
  String filename;

  public static AiTrainingFile getAiTrainingFile(File file) {
    return new AiTrainingFile(file.getId(), file.getCreatedAt(), file.getFilename());
  }
}
