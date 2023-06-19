package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.entities.json.AiSuggestionRequest;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.image.Image;
import com.theokanning.openai.image.ImageResult;
import io.reactivex.Single;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class RestAiControllerTest {
  RestAiController controller;
  UserModel currentUser;

  Note note;
  @Mock OpenAiApi openAiApi;
  @Autowired MakeMe makeMe;

  AiSuggestionRequest params =
      new AiSuggestionRequest() {
        {
          this.prompt = "describe Earth";
        }
      };

  @BeforeEach
  void Setup() {
    currentUser = makeMe.aUser().toModelPlease();
    note = makeMe.aNote().please();
    controller = new RestAiController(openAiApi, makeMe.modelFactoryService, currentUser);
  }

  @Nested
  class AskSuggestion {
    @Test
    void askWithNoteThatCannotAccess() {
      assertThrows(
          ResponseStatusException.class,
          () ->
              new RestAiController(openAiApi, makeMe.modelFactoryService, makeMe.aNullUserModel())
                  .askSuggestion(note, params));
    }

    @Test
    void askSuggestionWithRightPrompt() {
      Note cosmos = makeMe.aNote("cosmos").please();
      Note solar = makeMe.aNote("solar system").under(cosmos).please();
      Note earth = makeMe.aNote("Earth").under(solar).please();
      when(openAiApi.createChatCompletion(
              argThat(
                  request -> {
                    assertEquals("describe Earth", request.getMessages().get(1).getContent());
                    assertEquals(
                        "context: cosmos › solar system",
                        request.getMessages().get(0).getContent());
                    return true;
                  })))
          .thenReturn(buildCompletionResult("blue planet"));
      controller.askSuggestion(earth, params);
    }

    @Test
    void askSuggestionAndUseResponse() {
      when(openAiApi.createChatCompletion(any())).thenReturn(buildCompletionResult("blue planet"));
      AiSuggestion aiSuggestion = controller.askSuggestion(note, params);
      assertEquals("blue planet", aiSuggestion.getSuggestion());
    }
  }

  @Nested
  class AskEngagingStory {
    Note aNote;

    @BeforeEach
    void setup() {
      aNote = makeMe.aNote("sanskrit").creatorAndOwner(currentUser).please();
    }

    @Test
    void askWithNoteThatCannotAccess() {
      assertThrows(
          ResponseStatusException.class,
          () ->
              new RestAiController(openAiApi, makeMe.modelFactoryService, makeMe.aNullUserModel())
                  .askEngagingStories(params));
    }

    @Test
    void askEngagingStoryWithRightPrompt() {
      when(openAiApi.createImage(
              argThat(
                  request -> {
                    assertEquals("describe Earth", request.getPrompt());
                    return true;
                  })))
          .thenReturn(buildImageResult("This is an engaging story."));
      controller.askEngagingStories(params);
    }

    @Test
    void askEngagingStoryReturnsEngagingStory() throws UnexpectedNoAccessRightException {
      when(openAiApi.createImage(Mockito.any()))
          .thenReturn(buildImageResult("This is an engaging story."));
      final AiEngagingStory aiEngagingStory = controller.askEngagingStories(params);
      assertEquals("This is an engaging story.", aiEngagingStory.engagingStory());
    }
  }

  private Single<ImageResult> buildImageResult(String s) {
    ImageResult result = new ImageResult();
    Image image = new Image();
    image.setB64Json(s);
    result.setData(List.of(image));
    return Single.just(result);
  }

  @NotNull
  private Single<ChatCompletionResult> buildCompletionResult(String text) {
    return Single.just(makeMe.openAiCompletionResult().choice(text).please());
  }
}
