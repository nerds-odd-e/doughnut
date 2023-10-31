package com.odde.doughnut.controllers;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.OpenAiResponse;
import com.theokanning.openai.file.File;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import static java.util.Collections.singletonList;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
class RestAiControllerTrainingFileTest {

  @Mock
  private OpenAiApi openAiApi;
  @Autowired
  MakeMe makeMe;

  RestAiController controller;



  @Disabled
  @Test
  void retrieveTrainingFiles() {

    File file = new File();
    file.setId("id");
    file.setObject("obj");
    file.setBytes(1L);
    file.setCreatedAt(1L);
    file.setFilename("filename");
    file.setPurpose("purpose");

    OpenAiResponse<File> openAiResponse = new OpenAiResponse<File>();
    openAiResponse.setData(singletonList(file));

    Mockito.when(openAiApi.listFiles()).thenReturn(Single.just(openAiResponse));



  }
}
