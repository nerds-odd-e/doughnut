package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookAssistant;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.AssistantCreationService;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.assistants.assistant.Assistant;
import com.theokanning.openai.assistants.assistant.AssistantRequest;
import com.theokanning.openai.assistants.vector_store.VectorStore;
import com.theokanning.openai.assistants.vector_store_file.VectorStoreFile;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.file.File;
import io.reactivex.Single;
import java.io.IOException;
import java.util.Map;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.Buffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestAiAssistantCreationControllerTest {
  RestAiAssistantCreationController controller;
  UserModel currentUser;
  @Mock OpenAiApi openAiApi;
  @Autowired MakeMe makeMe;
  TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void Setup() {
    currentUser = makeMe.anAdmin().toModelPlease();
    controller = createController(currentUser);
  }

  private RestAiAssistantCreationController createController(UserModel user) {
    return new RestAiAssistantCreationController(
        new AssistantCreationService(openAiApi),
        new GlobalSettingsService(makeMe.modelFactoryService),
        makeMe.modelFactoryService,
        user,
        testabilitySettings);
  }

  // Move relevant tests from RestAiControllerTest here
  @Nested
  class CreateDefaultAssistants {

    @BeforeEach
    void setup() {
      Assistant assistantToReturn = new Assistant();
      assistantToReturn.setId("created-assistant-id");
      assistantToReturn.setName("Assistant created");
      when(openAiApi.createAssistant(ArgumentMatchers.any()))
          .thenReturn(Single.just(assistantToReturn));
    }

    @Nested
    class recreateAllAssistants {
      @Test
      void authentication() {
        controller = createController(makeMe.aUser().toModelPlease());
        assertThrows(
            UnexpectedNoAccessRightException.class, () -> controller.recreateDefaultAssistant());
      }

      @Test
      void createCompletionAssistant() throws UnexpectedNoAccessRightException {
        Map<String, String> result = controller.recreateDefaultAssistant();
        assertThat(result.get("Assistant created")).isEqualTo("created-assistant-id");
        GlobalSettingsService globalSettingsService =
            new GlobalSettingsService(makeMe.modelFactoryService);
        assertThat(globalSettingsService.defaultAssistantId().getValue())
            .isEqualTo("created-assistant-id");
      }
    }
  }

  @Nested
  class createNotebookAssistant {
    Notebook notebook;
    String uploadedFileContent = "";

    @BeforeEach
    public void setup() {
      notebook = makeMe.aNotebook().please();
      Assistant assistantToReturn = new Assistant();
      assistantToReturn.setId("created-assistant-id");
      assistantToReturn.setName("Assistant created");
      when(openAiApi.createAssistant(ArgumentMatchers.any()))
          .thenReturn(Single.just(assistantToReturn));
      VectorStore vectorStore = new VectorStore();
      vectorStore.setId("new-vector-store-id");
      when(openAiApi.createVectorStore(ArgumentMatchers.any()))
          .thenReturn(Single.just(vectorStore));
      VectorStoreFile vectorStoreFile = new VectorStoreFile();
      vectorStoreFile.setId("new-vector-store-file-id");
      when(openAiApi.createVectorStoreFile(eq("new-vector-store-id"), ArgumentMatchers.any()))
          .thenReturn(Single.just(vectorStoreFile));
      when(openAiApi.uploadFile(any(RequestBody.class), any(MultipartBody.Part.class)))
          .then(
              (invocation) -> {
                uploadedFileContent = getBuffer(invocation.getArgument(1));
                File item = new File();
                item.setId("new-file-id");
                return Single.just(item);
              });
    }

    @Test
    void authentication() {
      controller = createController(makeMe.aUser().toModelPlease());
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.recreateNotebookAssistant(notebook));
    }

    @Test
    void createNotebookAssistant() throws UnexpectedNoAccessRightException, IOException {
      NotebookAssistant notebookAssistant = controller.recreateNotebookAssistant(notebook);
      assertThat(notebookAssistant.getCreatedAt()).isNotNull();
      assertThat(notebookAssistant.getCreator()).isEqualTo(currentUser.getEntity());
      assertThat(notebookAssistant.getId()).isNotNull();
    }

    @Test
    void useTheCreatedAssistantId() throws UnexpectedNoAccessRightException, IOException {
      NotebookAssistant notebookAssistant = controller.recreateNotebookAssistant(notebook);
      assertThat(notebookAssistant.getAssistantId()).isEqualTo("created-assistant-id");
    }

    @Test
    void passTheRightParameters() throws UnexpectedNoAccessRightException, IOException {
      controller.recreateNotebookAssistant(notebook);
      ArgumentCaptor<AssistantRequest> captor = ArgumentCaptor.forClass(AssistantRequest.class);
      verify(openAiApi).createAssistant(captor.capture());
      assertThat(captor.getValue().getName()).startsWith("Assistant for notebook ");
      assertThat(captor.getValue().getTools().get(0).getType()).startsWith("function");
      assertThat(captor.getValue().getTools().get(4).getType()).startsWith("file_search");
      assertThat(captor.getValue().getToolResources().getFileSearch().getVectorStoreIds().get(0))
          .isEqualTo("new-vector-store-id");
    }

    @Test
    void uploadAllNotes() throws UnexpectedNoAccessRightException, IOException {
      Note child = makeMe.aNote().under(notebook.getHeadNote()).please();
      controller.recreateNotebookAssistant(notebook);
      assertThat(uploadedFileContent).contains(notebook.getHeadNote().getTopicConstructor());
      assertThat(uploadedFileContent).contains(child.getTopicConstructor());
    }

    @Test
    void shouldNotCreateNewRecordIfExist() throws UnexpectedNoAccessRightException, IOException {
      NotebookAssistant notebookAssistant = new NotebookAssistant();
      notebookAssistant.setAssistantId("previous-assistant-id");
      notebookAssistant.setCreatedAt(makeMe.aTimestamp().please());
      notebookAssistant.setCreator(currentUser.getEntity());
      notebookAssistant.setNotebook(notebook);
      makeMe.modelFactoryService.save(notebookAssistant);
      makeMe.refresh(notebook);
      controller.recreateNotebookAssistant(notebook);
      makeMe.refresh(notebookAssistant);
      assertThat(notebookAssistant.getAssistantId()).isEqualTo("created-assistant-id");
    }

    private static String getBuffer(MultipartBody.Part part) {
      RequestBody requestBody = part.body();
      Buffer buffer = new Buffer();
      try {
        requestBody.writeTo(buffer);
        return buffer.readUtf8();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
