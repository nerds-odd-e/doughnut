package com.odde.doughnut.controllers;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import com.odde.doughnut.controllers.json.AiTrainingFile;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.OpenAiResponse;
import com.theokanning.openai.file.File;
import com.theokanning.openai.fine_tuning.FineTuningJob;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.reactivex.Single;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class RestAiControllerTrainingFileTest {

  @Autowired MakeMe makeMe;
  RestAiController controller;
  UserModel currentUser;
  @Mock private OpenAiApi openAiApi;

  @BeforeEach
  void setUp() {
    currentUser = makeMe.aUser().toModelPlease();
    controller = new RestAiController(openAiApi, makeMe.modelFactoryService, currentUser);
  }

  @Test
  void retrieveTrainingFiles() {

    OpenAiResponse<File> openAiResponse = getOpenAiFiles();

    Mockito.when(openAiApi.listFiles()).thenReturn(Single.just(openAiResponse));

    List<AiTrainingFile> aiTrainingFiles = controller.retrieveTrainingFiles();
    assertEquals(2, aiTrainingFiles.size());
  }

  @Test
  void triggerAiModelFineTuning() {

    Mockito.when(openAiApi.createFineTuningJob(any()))
      .thenReturn(Single.just(new FineTuningJob() {{
      setStatus("queued");
    }}));

    controller.triggerFineTune("test");

    Mockito.verify(openAiApi).createFineTuningJob(argThat(argument -> argument.getTrainingFile().equals("test")));

  }

  @NotNull
  private OpenAiResponse<File> getOpenAiFiles() {
    File file = new File();
    file.setId("id");
    file.setObject("obj");
    file.setBytes(1L);
    file.setCreatedAt(1L);
    file.setFilename("filename");
    file.setPurpose("purpose");

    File file1 = new File();
    file.setId("id1");
    file.setObject("obj");
    file.setBytes(1L);
    file.setCreatedAt(2L);
    file.setFilename("filename1");
    file.setPurpose("purpose");

    OpenAiResponse<File> openAiResponse = new OpenAiResponse<>();
    openAiResponse.setData(List.of(file, file1));
    return openAiResponse;
  }
}
