package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.OpenAiResponse;
import com.theokanning.openai.file.File;
import com.theokanning.openai.fine_tuning.FineTuningJob;
import io.reactivex.Single;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

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
  void triggerAiModelFineTuning() {

    Mockito.when(openAiApi.createFineTuningJob(any()))
        .thenReturn(
            Single.just(
                new FineTuningJob() {
                  {
                    setStatus("queued");
                  }
                }));

    controller.triggerFineTune("test");

    Mockito.verify(openAiApi)
        .createFineTuningJob(
            argThat(
                argument ->
                    argument.getTrainingFile().equals("test")
                        && argument.getModel().equals("gpt-3.5-turbo-1106")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"failed", "cancelled"})
  void throwExceptionWhenTuningStatusIsFailOrCancelled(String status) {

    Single<FineTuningJob> response =
        Single.just(
            new FineTuningJob() {
              {
                setStatus(status);
              }
            });
    Mockito.when(openAiApi.createFineTuningJob(any())).thenReturn(response);

    assertEquals("Failed", controller.triggerFineTune("test").message);
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
    file1.setId("id1");
    file1.setObject("obj");
    file1.setBytes(1L);
    file1.setCreatedAt(2L);
    file1.setFilename("filename1");
    file1.setPurpose("purpose");

    OpenAiResponse<File> openAiResponse = new OpenAiResponse<>();
    openAiResponse.setData(List.of(file, file1));
    return openAiResponse;
  }
}
