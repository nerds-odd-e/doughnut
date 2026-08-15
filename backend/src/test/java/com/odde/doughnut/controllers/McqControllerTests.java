package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Mcq;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.OpenAiNotAvailableException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
  class HttpPaths {
    @Test
    void noteMcqRoutesAreUnderMcqs() throws Exception {
      assertThat(classPath(), equalTo("/api/mcqs"));
      assertThat(
          classPath() + getPath("getAllQuestionByNote", Note.class),
          equalTo("/api/mcqs/{note}/note-questions"));
      assertThat(
          classPath() + postPath("addQuestionManually", Note.class, Mcq.class),
          equalTo("/api/mcqs/{note}/note-questions"));
      assertThat(
          classPath() + postPath("generateQuestionWithoutSave", Note.class),
          equalTo("/api/mcqs/generate-question-without-save"));
      assertThat(
          classPath() + getPath("exportQuestionGeneration", Note.class),
          equalTo("/api/mcqs/{note}/export-question-generation"));
      assertThat(
          classPath() + postPath("refineQuestion", Note.class, Mcq.class),
          equalTo("/api/mcqs/{note}/refine-question"));
    }

    private String classPath() {
      return McqController.class.getAnnotation(RequestMapping.class).value()[0];
    }

    private String getPath(String method, Class<?>... params) throws Exception {
      return McqController.class
          .getDeclaredMethod(method, params)
          .getAnnotation(GetMapping.class)
          .value()[0];
    }

    private String postPath(String method, Class<?>... params) throws Exception {
      return McqController.class
          .getDeclaredMethod(method, params)
          .getAnnotation(PostMapping.class)
          .value()[0];
    }
  }

  @Nested
  class GetListOfMcqForNotebook {
    @Test
    void authorization() {
      Note note = makeMe.aNote().please();
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.getAllQuestionByNote(note));
    }

    @Test
    void getMcqsWhenThereAreNone() throws UnexpectedNoAccessRightException {
      assertThat(controller.getAllQuestionByNote(ownedNote()), hasSize(0));
    }

    @Test
    void getMcqsWhenThereIsOne() throws UnexpectedNoAccessRightException {
      Note note = ownedNote();
      Mcq mcq = makeMe.anMcq().ofAIGeneratedQuestionForNote(note).please();
      makeMe.refresh(note);

      assertThat(controller.getAllQuestionByNote(note), contains(mcq));
    }

    @Test
    void getMcqsWhenThereAreSeveral() throws UnexpectedNoAccessRightException {
      Note note = makeMe.theNote(ownedNote()).hasAnMcq().please();
      makeMe.anMcq().ofAIGeneratedQuestionForNote(note).please();
      makeMe.refresh(note);

      assertThat(controller.getAllQuestionByNote(note), hasSize(2));
    }
  }

  @Nested
  class addQuestionToNote {
    @Test
    void authorization() {
      Note note = makeMe.aNote().please();
      Mcq mcqWithAnswer = makeMe.anMcq().please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.addQuestionManually(note, mcqWithAnswer));
    }

    @Test
    void persistent() throws UnexpectedNoAccessRightException {
      Note note = ownedNote();
      controller.addQuestionManually(note, makeMe.anMcq().please());
      makeMe.refresh(note);
      assertThat(note.getMcqs(), hasSize(1));
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
