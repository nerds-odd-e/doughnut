package com.odde.doughnut.controllers;

import static com.odde.doughnut.controllers.dto.ApiError.ErrorType.ASSESSMENT_SERVICE_ERROR;
import static java.util.stream.Collectors.toList;

import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.SearchTermModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.QuizQuestionService;
import com.theokanning.openai.client.OpenAiApi;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    List<Note> notes = searchTermModel.search(notebook.getId()).limit(5).collect((toList()));

    if (notes.size() < 5) {
      return new ArrayList<>();
    }

    return notes.stream().map(quizQuestionService::generateAIQuestion).collect((toList()));
  }

  public List<QuizQuestion> generateAssessmentQuestions(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    currentUser.assertLoggedIn();
    currentUser.assertReadAuthorization(notebook);

    List<QuizQuestion> approvedQuestionList =
        quizQuestionService.getApprovedAssessmentQuestion(notebook.getHeadNote().getChildren());

    List<QuizQuestion> filteredQuestionList =
        approvedQuestionList.stream()
            .collect(
                Collectors.groupingBy(
                    s -> s.getNote().getId(),
                    Collectors.collectingAndThen(
                        Collectors.mapping(q -> q, Collectors.toList()), List::getFirst)))
            .values()
            .stream()
            .toList();

    if (filteredQuestionList.size() < 5) {
      throw new ApiException(
          "Not enough approved questions",
          ASSESSMENT_SERVICE_ERROR,
          "Not enough approved questions");
    }

    return filteredQuestionList;
  }
}
