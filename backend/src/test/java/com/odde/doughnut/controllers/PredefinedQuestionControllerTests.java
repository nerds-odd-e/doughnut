package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.OpenAiNotAvailableException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PredefinedQuestionControllerTests extends ControllerTestBase {

  @Autowired PredefinedQuestionController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  PredefinedQuestionController nullUserController() {
    currentUser.setUser(null);
    return controller;
  }

  @Nested
  class GetListOfPredefinedQuestionForNotebook {
    Note noteWithoutQuestions;
    Note noteWithQuestions;

    @BeforeEach
    void setUp() {
      Notebook readingNb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      Note rootNote = makeMe.aRootNote("My reading list").notebook(readingNb).please();
      makeMe.theNote(rootNote).withNChildren(10).please();
      noteWithoutQuestions =
          makeMe.aNote("Zen and the Art of Motorcycle Maintenance").notebook(readingNb).please();
      Notebook lilaNb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      Note lila = makeMe.aNote("Lila").notebook(lilaNb).please();
      noteWithQuestions = makeMe.theNote(lila).hasAPredefinedQuestion().please();
    }

    @Test
    void authorization() {
      Note note = makeMe.aNote().please();
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.getAllQuestionByNote(note));
    }

    @Test
    void getQuestionsOfANoteWhenThereIsNotQuestion() throws UnexpectedNoAccessRightException {
      List<PredefinedQuestion> results = controller.getAllQuestionByNote(noteWithoutQuestions);
      assertThat(results, hasSize(0));
    }

    @Test
    void getQuestionsOfANoteWhenThereIsOneQuestion() throws UnexpectedNoAccessRightException {
      PredefinedQuestion questionOfNote =
          makeMe.aPredefinedQuestion().ofAIGeneratedQuestionForNote(noteWithoutQuestions).please();
      makeMe.refresh(noteWithoutQuestions);
      List<PredefinedQuestion> results = controller.getAllQuestionByNote(noteWithoutQuestions);
      assertThat(results, contains(questionOfNote));
    }

    @Test
    void getAllQuestionsOfANoteWhenThereIsMoreThanOneQuestion()
        throws UnexpectedNoAccessRightException {
      makeMe.aPredefinedQuestion().ofAIGeneratedQuestionForNote(noteWithQuestions).please();
      makeMe.refresh(noteWithQuestions);
      List<PredefinedQuestion> results = controller.getAllQuestionByNote(noteWithQuestions);
      assertThat(results, hasSize(2));
    }
  }

  @Nested
  class addQuestionToNote {
    @Test
    void authorization() {
      Note note = makeMe.aNote().please();
      PredefinedQuestion mcqWithAnswer = makeMe.aPredefinedQuestion().please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.addQuestionManually(note, mcqWithAnswer));
    }

    @Test
    void persistent() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      PredefinedQuestion mcqWithAnswer = makeMe.aPredefinedQuestion().please();
      controller.addQuestionManually(note, mcqWithAnswer);
      makeMe.refresh(note);
      assertThat(note.getPredefinedQuestions(), hasSize(1));
    }
  }

  @Nested
  class GenerateQuestionWithoutSave {
    @Test
    void shouldThrowWhenOpenAiNotAvailable() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      testabilitySettings.setOpenAiTokenOverride("");
      assertThrows(
          OpenAiNotAvailableException.class, () -> controller.generateQuestionWithoutSave(note));
    }
  }

  @Nested
  class ExportQuestionGeneration {
    Note note;

    @BeforeEach
    void setup() {
      note =
          makeMe
              .aNote()
              .notebookOwnedBy(currentUser.getUser())
              .title("There are 42 prefectures in Japan")
              .please();
    }

    @Test
    void shouldNotBeAbleToExportQuestionGenerationForNoteIAmNotAuthorized() {
      Note otherNote = makeMe.aNote().please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.exportQuestionGeneration(otherNote));
    }

    @Test
    void shouldExportQuestionGenerationWithAllNonEmptyFields()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      java.util.Map<String, Object> request = controller.exportQuestionGeneration(note);
      assertThat(request, notNullValue());
      assertThat(request.containsKey("model"), is(true));
      assertThat(request.get("model"), notNullValue());
      assertThat(request.containsKey("instructions"), is(true));
      assertThat(request.get("instructions"), notNullValue());
      assertThat(request.containsKey("input"), is(true));
      assertThat(request.get("input"), notNullValue());
      assertThat(request.containsKey("text"), is(true));
      assertThat(request.get("text"), notNullValue());
      assertThat(request.containsKey("max_output_tokens"), is(true));
      assertThat(request.get("max_output_tokens"), is(1000));
      // Verify the JSON is not empty
      String jsonString = new ObjectMapperConfig().objectMapper().writeValueAsString(request);
      assertThat(jsonString, not(equalTo("{}")));
      assertThat(jsonString.length(), greaterThan(10));
      java.util.List<String> validFields = findValidFields(request);
      assertThat(
          "Exported question generation should not contain 'valid' fields, but found: "
              + validFields,
          validFields,
          empty());
    }

    private java.util.List<String> findValidFields(Object obj) {
      java.util.List<String> validFields = new java.util.ArrayList<>();
      findValidFieldsRecursive(obj, "", validFields);
      return validFields;
    }

    @SuppressWarnings("unchecked")
    private void findValidFieldsRecursive(
        Object obj, String path, java.util.List<String> validFields) {
      if (obj == null) {
        return;
      }
      if (obj instanceof java.util.Map) {
        java.util.Map<String, Object> map = (java.util.Map<String, Object>) obj;
        for (java.util.Map.Entry<String, Object> entry : map.entrySet()) {
          String key = entry.getKey();
          Object value = entry.getValue();
          String currentPath = path.isEmpty() ? key : path + "." + key;
          if ("valid".equals(key)) {
            validFields.add(currentPath);
          }
          findValidFieldsRecursive(value, currentPath, validFields);
        }
      } else if (obj instanceof java.util.List) {
        java.util.List<?> list = (java.util.List<?>) obj;
        for (int i = 0; i < list.size(); i++) {
          findValidFieldsRecursive(list.get(i), path + "[" + i + "]", validFields);
        }
      }
    }
  }
}
