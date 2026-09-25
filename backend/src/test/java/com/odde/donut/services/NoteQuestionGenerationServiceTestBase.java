package com.odde.donut.services;

import com.odde.donut.entities.Note;
import com.odde.donut.services.ai.GeneratedMcq;
import com.odde.donut.testability.OpenAiStructuredResponseMock;
import com.odde.donut.testability.SpringTestBase;
import com.openai.models.responses.StructuredResponseCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

abstract class NoteQuestionGenerationServiceTestBase extends SpringTestBase {
  @Autowired GlobalSettingsService globalSettingsService;
  @Autowired NoteQuestionGenerationService service;
  OpenAiStructuredResponseMock openAiStructuredResponseMock;
  Note testNote;

  @BeforeEach
  void setup() {
    openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);
    testNote = makeMe.aNote().please();
  }

  String instructionText(StructuredResponseCreateParams<GeneratedMcq> request) {
    return request.rawParams().instructions().orElse("");
  }

  String inputText(StructuredResponseCreateParams<GeneratedMcq> request) {
    return request.rawParams().input().flatMap(input -> input.text()).orElse("");
  }

  String modelName(StructuredResponseCreateParams<GeneratedMcq> request) {
    return request.rawParams().model().orElseThrow().asString();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  ArgumentCaptor<StructuredResponseCreateParams<GeneratedMcq>> responseParamsCaptor() {
    return ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
  }
}
