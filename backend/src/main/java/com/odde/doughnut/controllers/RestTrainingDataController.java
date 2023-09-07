package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.json.GoodTrainingData;
import com.odde.doughnut.entities.json.TrainingDataMessage;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ai.OpenAIChatAboutNoteRequestBuilder;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/gettrainingdata")
class RestTrainingDataController {
  private final ModelFactoryService modelFactoryService;
  private UserModel currentUser;

  public RestTrainingDataController(
      ModelFactoryService modelFactoryService, UserModel currentUser) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
  }

  @GetMapping("/goodtrainingdata")
  public List<GoodTrainingData> getGoodTrainingData() {
    currentUser.assertLoggedIn();
    List<GoodTrainingData> goodTrainingDataList = new ArrayList<>();
    modelFactoryService
        .markedQuestionRepository
        .findAll()
        .forEach(
            markedQuestion -> {
              Note note =
                  modelFactoryService.noteRepository.findById(markedQuestion.getNoteId()).get();
              QuizQuestionEntity quizQuestion =
                  modelFactoryService
                      .quizQuestionRepository
                      .findById(markedQuestion.getQuizQuestionId())
                      .get();
              ChatCompletionRequest chatRequest =
                  new OpenAIChatAboutNoteRequestBuilder()
                      .contentOfNoteOfCurrentFocus(note)
                      .userInstructionToGenerateQuestionWithGPT35FineTunedModel()
                      .build();
              goodTrainingDataList.add(
                  generateGoodTrainingData(chatRequest.getMessages(), quizQuestion));
            });
    return goodTrainingDataList;
  }

  public GoodTrainingData generateGoodTrainingData(
      List<ChatMessage> messages, QuizQuestionEntity quizQuestion) {
    GoodTrainingData goodTrainingData = new GoodTrainingData();
    List<TrainingDataMessage> trainingDataMessageList =
        messages.stream()
            .filter(
                chatMessage ->
                    (chatMessage.getRole().equalsIgnoreCase(ChatMessageRole.SYSTEM.value())
                            && chatMessage.getContent().contains("The note of current focus"))
                        || (chatMessage.getRole().equalsIgnoreCase(ChatMessageRole.USER.value())))
            .map(
                chatMessage -> {
                  TrainingDataMessage trainingDataMessage = new TrainingDataMessage();
                  trainingDataMessage.setRole(chatMessage.getRole());
                  trainingDataMessage.setContent(chatMessage.getContent());
                  return trainingDataMessage;
                })
            .collect(Collectors.toList());
    trainingDataMessageList.add(
        new TrainingDataMessage(
            ChatMessageRole.ASSISTANT.value(), quizQuestion.getRawJsonQuestion()));
    goodTrainingData.addTrainingDataMessages(trainingDataMessageList);
    return goodTrainingData;
  }
}
