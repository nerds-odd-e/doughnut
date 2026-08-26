package com.odde.donut.services;

import com.odde.donut.entities.Note;
import com.odde.donut.services.ai.GeneratedMcq;
import com.odde.donut.testability.MakeMe;
import com.odde.donut.testability.OpenAiStructuredResponseMock;
import com.openai.client.OpenAIClient;
import com.openai.models.responses.StructuredResponseCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
abstract class NoteQuestionGenerationServiceTestBase {

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @Autowired MakeMe makeMe;
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
