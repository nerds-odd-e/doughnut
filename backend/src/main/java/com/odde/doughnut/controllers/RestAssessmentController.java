package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.SearchTermModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.QuizQuestionService;
import com.theokanning.openai.client.OpenAiApi;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/assessment")
class RestAssessmentController {
  private final QuizQuestionService quizQuestionService;
  private final UserModel currentUser;

  private final ModelFactoryService modelFactoryService;

  public RestAssessmentController(
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      ModelFactoryService modelFactoryService,
      UserModel currentUser) {
    this.currentUser = currentUser;
    this.modelFactoryService = modelFactoryService;
    this.quizQuestionService = new QuizQuestionService(openAiApi, modelFactoryService);
  }

  @PostMapping("/ai-questions/{notebook}")
  @Transactional
  public List<QuizQuestion> generateAiQuestions(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    currentUser.assertLoggedIn();
    currentUser.assertReadAuthorization(notebook);
    SearchTermModel searchTermModel =
        this.modelFactoryService.toSearchTermModel(currentUser.getEntity(), new SearchTerm());

    List<Note> notes =
        searchTermModel.search(notebook.getId()).limit(5).collect((Collectors.toList()));

    if (notes.size() < 5) {
      throw new ResponseStatusException(
          HttpStatusCode.valueOf(500),
          "Notebook has less than 5 notes. Unable to generate sufficient quiz questions");
    }

    return notes.stream()
        .map(quizQuestionService::generateAIQuestion)
        .collect((Collectors.toList()));
  }

  public List<QuizQuestion> generateAssessment(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
<<<<<<< HEAD
//    currentUser.assertLoggedIn();
//    currentUser.assertReadAuthorization(notebook);
//
//    //get 5 random notes, random approved questions
//    List<Note> notes = notebook.getHeadNote().getDescendants().toList();
//
//    if (notes.size() < 5) {
//      throw new ResponseStatusException(
//        HttpStatusCode.valueOf(500),
//        "Notebook has less than 5 notes. Unable to generate sufficient quiz questions");
//    }
=======
        currentUser.assertLoggedIn();
    //    currentUser.assertReadAuthorization(notebook);
    //
    //    //get 5 random notes, random approved questions
    //    List<Note> notes = notebook.getHeadNote().getDescendants().toList();
    //
    //    if (notes.size() < 5) {
    //      throw new ResponseStatusException(
    //        HttpStatusCode.valueOf(500),
    //        "Notebook has less than 5 notes. Unable to generate sufficient quiz questions");
    //    }
>>>>>>> a355e1f25 (add test to validate login)

    QuizQuestion question = new QuizQuestion();
    question.approved = true;
    return Arrays.asList(question,question,question,question,question);
  }
}
