package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.assistants.assistant.Assistant;
import com.theokanning.openai.client.OpenAiApi;
import io.reactivex.Single;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestAiControllerForAssistantTest {
  RestAiController controller;
  UserModel currentUser;

  Note note;
  @Mock OpenAiApi openAiApi;
  @Autowired MakeMe makeMe;
  TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void Setup() {
    currentUser = makeMe.anAdmin().toModelPlease();
    note = makeMe.aNote().please();
    controller =
        new RestAiController(
            openAiApi, makeMe.modelFactoryService, currentUser, testabilitySettings);
  }

  @Nested
  class CreateDefaultAssistants {

    @BeforeEach
    void setup() {
      Assistant assistantToReturn = new Assistant();
      assistantToReturn.setId("1234");
      when(openAiApi.createAssistant(ArgumentMatchers.any()))
          .thenReturn(Single.just(assistantToReturn));
    }

    @Nested
    class recreateAllAssistants {
      @Test
      void authentication() {
        controller =
            new RestAiController(
                openAiApi,
                makeMe.modelFactoryService,
                makeMe.aUser().toModelPlease(),
                testabilitySettings);
        assertThrows(
            UnexpectedNoAccessRightException.class, () -> controller.recreateAllAssistants());
      }

      @Test
      void createCompletionAssistant() throws UnexpectedNoAccessRightException {
        Map<String, String> result = controller.recreateAllAssistants();
        assertThat(result.get("Note details completion")).isEqualTo("1234");
        GlobalSettingsService globalSettingsService =
            new GlobalSettingsService(makeMe.modelFactoryService);
        assertThat(globalSettingsService.noteCompletionAssistantId().getValue()).isEqualTo("1234");
      }

      @Test
      void createChatAssistant() throws UnexpectedNoAccessRightException {
        Map<String, String> result = controller.recreateAllAssistants();
        assertThat(result.get("chat assistant")).isEqualTo("1234");
        GlobalSettingsService globalSettingsService =
            new GlobalSettingsService(makeMe.modelFactoryService);
        assertThat(globalSettingsService.chatAssistantId().getValue()).isEqualTo("1234");
      }
    }

    @Nested
    class createNotebookAssistant {
      Notebook notebook;

      @BeforeEach
      public void setup() {
        notebook = note.getNotebook();
      }

      @Test
      void authentication() {
        controller =
            new RestAiController(
                openAiApi,
                makeMe.modelFactoryService,
                makeMe.aUser().toModelPlease(),
                testabilitySettings);
        assertThrows(
            UnexpectedNoAccessRightException.class,
            () -> controller.recreateNotebookAssistant(notebook));
      }

      @Test
      void createNotebookAssistant() throws UnexpectedNoAccessRightException {
        controller.recreateNotebookAssistant(notebook);
      }
    }
  }
}
