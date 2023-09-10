package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.MarkedQuestion;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.json.TrainingData;
import com.odde.doughnut.entities.json.TrainingDataMessage;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ai.OpenAIChatAboutNoteRequestBuilder;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
  public List<TrainingData> getGoodTrainingData() {
    currentUser.assertLoggedIn();
    List<MarkedQuestion> markedQuestions = new ArrayList<>();

    modelFactoryService.markedQuestionRepository.findAll().forEach(markedQuestions::add);

    return markedQuestions.stream().flatMap(this::getTrainingData).toList();
  }

  private Stream<TrainingData> getTrainingData(MarkedQuestion markedQuestion) {
    var chatRequest =
        new OpenAIChatAboutNoteRequestBuilder()
            .contentOfNoteOfCurrentFocus(markedQuestion.getNote())
            .userInstructionToGenerateQuestionWithGPT35FineTunedModel()
            .build();
    var possibleQuizQuestion =
        modelFactoryService.quizQuestionRepository.findById(
            markedQuestion.getQuizQuestion().getId());
    Optional<TrainingData> trainingData =
        possibleQuizQuestion.map(
            questionEntity -> generateTrainingData(chatRequest.getMessages(), questionEntity));
    return trainingData.stream();
  }

  public TrainingData generateTrainingData(
      List<ChatMessage> messages, QuizQuestionEntity quizQuestion) {
    TrainingData goodTrainingData = new TrainingData();
    List<TrainingDataMessage> trainingDataMessages =
        messages.stream()
            .filter(this::isValidChatMessage)
            .map(this::createNewTrainingDataMessage)
            .collect(Collectors.toList());
    var tdm =
        new TrainingDataMessage(
            ChatMessageRole.ASSISTANT.value(), quizQuestion.getRawJsonQuestion());
    trainingDataMessages.add(tdm);
    goodTrainingData.addTrainingDataMessages(trainingDataMessages);
    return goodTrainingData;
  }

  private boolean isValidChatMessage(ChatMessage chatMessage) {
    return (chatMessage.getRole().equalsIgnoreCase(ChatMessageRole.SYSTEM.value())
            && chatMessage.getContent().contains("The note of current focus"))
        || (chatMessage.getRole().equalsIgnoreCase(ChatMessageRole.USER.value()));
  }

  private TrainingDataMessage createNewTrainingDataMessage(ChatMessage chatMessage) {
    TrainingDataMessage trainingDataMessage = new TrainingDataMessage();
    trainingDataMessage.setRole(chatMessage.getRole());
    trainingDataMessage.setContent(chatMessage.getContent());
    return trainingDataMessage;
  }
}
