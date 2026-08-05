package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.exceptions.OpenAiNotAvailableException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

  Note ownedNote() {
    return makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
  }

  Note ownedNote(String title) {
    return makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title(title).please();
  }

  @Nested
  class GetListOfPredefinedQuestionForNotebook {
    @Test
    void authorization() {
      Note note = makeMe.aNote().please();
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.getAllQuestionByNote(note));
    }

    @Test
    void getQuestionsOfANoteWhenThereIsNotQuestion() throws UnexpectedNoAccessRightException {
      assertThat(controller.getAllQuestionByNote(ownedNote()), hasSize(0));
    }

    @Test
    void getQuestionsOfANoteWhenThereIsOneQuestion() throws UnexpectedNoAccessRightException {
      Note note = ownedNote();
      PredefinedQuestion question =
          makeMe.aPredefinedQuestion().ofAIGeneratedQuestionForNote(note).please();
      makeMe.refresh(note);

      assertThat(controller.getAllQuestionByNote(note), contains(question));
    }

    @Test
    void getAllQuestionsOfANoteWhenThereIsMoreThanOneQuestion()
        throws UnexpectedNoAccessRightException {
      Note note = makeMe.theNote(ownedNote()).hasAPredefinedQuestion().please();
      makeMe.aPredefinedQuestion().ofAIGeneratedQuestionForNote(note).please();
      makeMe.refresh(note);

      assertThat(controller.getAllQuestionByNote(note), hasSize(2));
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
      Note note = ownedNote();
      controller.addQuestionManually(note, makeMe.aPredefinedQuestion().please());
      makeMe.refresh(note);
      assertThat(note.getPredefinedQuestions(), hasSize(1));
    }
  }

  @Nested
  class GenerateQuestionWithoutSave {
    @Test
    void shouldThrowWhenOpenAiNotAvailable() {
      Note note = ownedNote();
      testabilitySettings.setOpenAiTokenOverride("");
      assertThrows(
          OpenAiNotAvailableException.class, () -> controller.generateQuestionWithoutSave(note));
    }
  }

  @Nested
  class ExportQuestionGeneration {
    @Test
    void shouldNotBeAbleToExportQuestionGenerationForNoteIAmNotAuthorized() {
      Note otherNote = makeMe.aNote().please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.exportQuestionGeneration(otherNote));
    }

    @Test
    void shouldExportQuestionGenerationWithAllNonEmptyFields()
        throws UnexpectedNoAccessRightException {
      Note note = ownedNote("There are 42 prefectures in Japan");

      Map<String, Object> request = controller.exportQuestionGeneration(note);

      assertThat(request.keySet(), hasItems("model", "instructions", "input", "text"));
      assertThat(request.get("max_output_tokens"), is(1000));
      assertThat(
          request.get("input").toString(), containsString("There are 42 prefectures in Japan"));
      assertThat(findValidFields(request), empty());
    }

    private List<String> findValidFields(Object obj) {
      List<String> validFields = new ArrayList<>();
      findValidFieldsRecursive(obj, "", validFields);
      return validFields;
    }

    @SuppressWarnings("unchecked")
    private void findValidFieldsRecursive(Object obj, String path, List<String> validFields) {
      if (obj == null) {
        return;
      }
      if (obj instanceof Map) {
        Map<String, Object> map = (Map<String, Object>) obj;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
          String currentPath = path.isEmpty() ? entry.getKey() : path + "." + entry.getKey();
          if ("valid".equals(entry.getKey())) {
            validFields.add(currentPath);
          }
          findValidFieldsRecursive(entry.getValue(), currentPath, validFields);
        }
      } else if (obj instanceof List) {
        List<?> list = (List<?>) obj;
        for (int i = 0; i < list.size(); i++) {
          findValidFieldsRecursive(list.get(i), path + "[" + i + "]", validFields);
        }
      }
    }
  }
}
