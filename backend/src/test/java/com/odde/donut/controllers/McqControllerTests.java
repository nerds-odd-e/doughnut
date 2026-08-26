package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.OpenAiNotAvailableException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class McqControllerTests extends ControllerTestBase {

  @Autowired McqController controller;

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
  class ListMcqs {
    @Test
    void authorization() {
      Note note = makeMe.aNote().please();
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.list(note));
    }

    @Test
    void getMcqsWhenThereAreNone() throws UnexpectedNoAccessRightException {
      assertThat(controller.list(ownedNote()), hasSize(0));
    }

    @Test
    void getMcqsWhenThereIsOne() throws UnexpectedNoAccessRightException {
      Note note = ownedNote();
      Mcq mcq = makeMe.anMcq().forNote(note).please();
      makeMe.refresh(note);

      assertThat(controller.list(note), contains(mcq));
    }

    @Test
    void getMcqsWhenThereAreSeveral() throws UnexpectedNoAccessRightException {
      Note note = makeMe.theNote(ownedNote()).hasAnMcq().please();
      makeMe.anMcq().forNote(note).please();
      makeMe.refresh(note);

      assertThat(controller.list(note), hasSize(2));
    }
  }

  @Nested
  class AddMcq {
    @Test
    void authorization() {
      Note note = makeMe.aNote().please();
      Mcq mcq = makeMe.anMcq().please();
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.add(note, mcq));
    }

    @Test
    void persistent() throws UnexpectedNoAccessRightException {
      Note note = ownedNote();
      controller.add(note, makeMe.anMcq().please());
      makeMe.refresh(note);
      assertThat(note.getMcqs(), hasSize(1));
    }
  }

  @Nested
  class GenerateMcq {
    @Test
    void shouldThrowWhenOpenAiNotAvailable() {
      Note note = ownedNote();
      testabilitySettings.setOpenAiTokenOverride("");
      assertThrows(OpenAiNotAvailableException.class, () -> controller.generate(note));
    }
  }

  @Nested
  class ExportMcq {
    @Test
    void shouldNotBeAbleToExportQuestionGenerationForNoteIAmNotAuthorized() {
      Note otherNote = makeMe.aNote().please();
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.export(otherNote));
    }

    @Test
    void shouldExportQuestionGenerationWithAllNonEmptyFields()
        throws UnexpectedNoAccessRightException {
      Note note = ownedNote("There are 42 prefectures in Japan");

      Map<String, Object> request = controller.export(note);

      assertThat(request.keySet(), hasItems("model", "instructions", "input", "text"));
      assertThat(request.get("max_output_tokens"), is(1000));
      assertThat(
          request.get("input").toString(), containsString("There are 42 prefectures in Japan"));
      assertThat(findValidFields(request), empty());
      assertThat(request.toString(), containsString("correctAnswer"));
      assertThat(request.toString(), containsString("distractors"));
      assertThat(request.toString(), not(containsString("responseChoices")));
      assertThat(request.toString(), not(containsString("correctAnswerIndex")));
      assertThat(request.toString(), not(containsString("choicesMayBeShuffled")));
      assertThat(request.toString(), not(containsString("solutionChoiceIndex")));
      assertThat(request.toString(), not(containsString("MCQWithAnswer")));
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
