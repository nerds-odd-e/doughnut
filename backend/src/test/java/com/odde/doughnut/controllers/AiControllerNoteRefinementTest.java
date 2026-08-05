package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.NoteRefinementLayoutDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.ai.NoteRefinementLayout;
import com.odde.doughnut.services.ai.NoteRefinementLayoutItem;
import com.odde.doughnut.services.ai.NoteRefinementLayoutValidator;
import com.odde.doughnut.testability.OpenAiStructuredResponseMock;
import com.openai.client.OpenAIClient;
import com.openai.models.responses.ResponseTextConfig;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

class AiControllerNoteRefinementTest extends ControllerTestBase {
  @Autowired AiController controller;

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  OpenAiStructuredResponseMock openAiStructuredResponseMock;
  Note testNote;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    testNote = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);
  }

  @Nested
  class GenerateRefinementSuggestions {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldReturnEmptyListWhenNoteContentIsBlank(String content)
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      testNote.setContent(content);

      assertThat(controller.generateRefinementSuggestions(testNote).getItems()).isEmpty();
    }

    @Test
    void shouldCallResponsesApiWithStructuredInstructions()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      openAiStructuredResponseMock.stubStructuredResponse(
          new NoteRefinementLayout(
              List.of(
                  new NoteRefinementLayoutItem(
                      "p1",
                      "Point 1",
                      false,
                      List.of(
                          new NoteRefinementLayoutItem(
                              "p1-1", "[[Already extracted note]]", true, List.of()))),
                  new NoteRefinementLayoutItem("p2", "Point 2", false, List.of()))));
      testNote.setContent("Some note content");

      NoteRefinementLayoutDTO result = controller.generateRefinementSuggestions(testNote);

      assertThat(result.getItems()).hasSize(2);
      assertThat(result.getItems().getFirst().getText()).isEqualTo("Point 1");
      assertThat(result.getItems().getFirst().getChildren().getFirst().isAlreadyExtracted())
          .isTrue();

      @SuppressWarnings({"unchecked", "rawtypes"})
      ArgumentCaptor<StructuredResponseCreateParams<NoteRefinementLayout>> paramsCaptor =
          ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
      verify(openAiStructuredResponseMock.responseService()).create(paramsCaptor.capture());
      StructuredResponseCreateParams<NoteRefinementLayout> params = paramsCaptor.getValue();
      String instructions = params.rawParams().instructions().orElse("");
      assertThat(instructions)
          .contains("Return one current-content layout for the note content")
          .contains("not alternative breakdown suggestions")
          .contains("Do not create grandchildren")
          .contains("simple standalone wiki-link-only lines")
          .contains("Focus Note content only")
          .contains("only source for layout items")
          .contains("Retrieved Notes are secondary context only")
          .contains("do not add layout items for content that appears only in Retrieved Notes");
      assertThat(params.rawParams().text().flatMap(ResponseTextConfig::format)).isPresent();
      assertThat(params.rawParams().input().flatMap(input -> input.text()).orElse("")).isNotBlank();
      assertThat(params.rawParams().maxOutputTokens()).isEqualTo(Optional.of(1000L));
    }

    @Test
    void shouldReturnEmptyLayoutWhenAiReturnsInvalidLayout()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
      Logger validatorLogger = loggerContext.getLogger(NoteRefinementLayoutValidator.class);
      Level originalLevel = validatorLogger.getLevel();
      validatorLogger.setLevel(Level.ALL);
      ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
      logAppender.setContext(loggerContext);
      logAppender.start();
      validatorLogger.addAppender(logAppender);
      try {
        openAiStructuredResponseMock.stubStructuredResponse(
            new NoteRefinementLayout(
                List.of(
                    new NoteRefinementLayoutItem("same", "Point 1", false, List.of()),
                    new NoteRefinementLayoutItem("same", "Point 2", false, List.of()))));
        testNote.setContent("Some note content");

        assertThat(controller.generateRefinementSuggestions(testNote).getItems()).isEmpty();
        assertThat(logAppender.list)
            .anyMatch(
                event ->
                    event.getLevel() == Level.WARN
                        && event.getFormattedMessage().contains("duplicate item id"));
      } finally {
        logAppender.stop();
        validatorLogger.detachAppender(logAppender);
        validatorLogger.setLevel(originalLevel);
      }
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      currentUser.setUser(null);
      assertThrows(
          ResponseStatusException.class, () -> controller.generateRefinementSuggestions(testNote));
    }
  }
}
