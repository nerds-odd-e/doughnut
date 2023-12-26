package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.odde.doughnut.controllers.json.AiCompletionParams;
import com.odde.doughnut.controllers.json.ClarifyingQuestionAndAnswer;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.theokanning.openai.completion.chat.*;

import java.util.HashMap;
import java.util.List;

import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

public class OpenAIChatAboutNoteRequestBuilder1 extends OpenAIChatAboutNoteRequestBuilder {
  public OpenAIChatAboutNoteRequestBuilder1() {
    this.openAIChatRequestBuilder.addTextMessage(
      ChatMessageRole.SYSTEM,
      "This is a PKM system using hierarchical notes, each with a topic and details, to capture atomic concepts.");
  }
}

