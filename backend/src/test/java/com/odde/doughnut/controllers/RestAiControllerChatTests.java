package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.entities.json.ChatRequest;
import com.odde.doughnut.entities.json.ChatResponse;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import io.reactivex.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class RestAiControllerChatTests {

  @Mock private OpenAiApi openAiApi;

  @Autowired MakeMe makeMe;
  RestAiController controller;
  UserModel currentUser;

  @BeforeEach
  void setUp() {
    currentUser = makeMe.aUser().toModelPlease();
    controller = new RestAiController(openAiApi, makeMe.modelFactoryService, currentUser);
  }

  @Test
  void chatWithAI() {
    Single<ChatCompletionResult> completionResultSingle =
        Single.just(makeMe.openAiCompletionResult().choice("I'm ChatGPT").please());
    Mockito.when(openAiApi.createChatCompletion(Mockito.any())).thenReturn(completionResultSingle);
    ChatResponse res = controller.chat(new ChatRequest("What's your name?"));
    assertEquals("I'm ChatGPT", res.getAssistantMessage());
  }
}
