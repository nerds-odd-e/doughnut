package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.PropertyFocus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

class QuestionGenerationPropertyInstructionsTest extends NoteQuestionGenerationServiceTestBase {

  @Autowired QuestionGenerationRequestBuilder requestBuilder;

  @Nested
  class InstructionsByPropertyType {
    static final String PART_OF_LABEL =
        "Instruction from notebook \"Anatomy\" for property \"a part of\":";

    Note note;

    @BeforeEach
    void setup() {
      note = noteWithPartOfAndSimilarTo(notebookWithMap("a part of: PART_OF_INSTRUCTION"));
    }

    @ParameterizedTest
    @CsvSource(value = {"a part of 2,''", "a part of,[[X]]"})
    void propertyOfMatchingTypeCarriesTheEntryAfterTheGeneralInstruction(String key, String value) {
      String instructionMessage = firstUserMessage(note, new PropertyFocus(key, value));

      assertThat(instructionMessage, containsString(PART_OF_LABEL + "\nPART_OF_INSTRUCTION"));
      assertThat(
          instructionMessage.indexOf("GENERAL_INSTRUCTION"),
          lessThan(instructionMessage.indexOf(PART_OF_LABEL)));
    }

    @Test
    void wholeNoteQuestionHasOnlyTheGeneralInstruction() {
      String instructionMessage = firstUserMessage(note, null);

      assertThat(instructionMessage, containsString("GENERAL_INSTRUCTION"));
      assertThat(instructionMessage, not(containsString("PART_OF_INSTRUCTION")));
    }

    @Test
    void mapKeyMatchesIgnoringCase() {
      Note inOther = noteWithPartOfAndSimilarTo(notebookWithMap("A Part Of: PART_OF_INSTRUCTION"));

      assertThat(
          firstUserMessage(inOther, new PropertyFocus("a part of", "")),
          containsString(PART_OF_LABEL + "\nPART_OF_INSTRUCTION"));
    }

    @Test
    void blankEntryAddsNothing() {
      Note inOther = noteWithPartOfAndSimilarTo(notebookWithMap("a part of: \"  \""));

      String instructionMessage = firstUserMessage(inOther, new PropertyFocus("a part of", ""));

      assertThat(instructionMessage, containsString("GENERAL_INSTRUCTION"));
      assertThat(instructionMessage, not(containsString("for property")));
    }

    @Test
    void propertyOfOtherTypeHasOnlyTheGeneralInstruction() {
      String instructionMessage = firstUserMessage(note, new PropertyFocus("similar to", ""));

      assertThat(instructionMessage, containsString("GENERAL_INSTRUCTION"));
      assertThat(instructionMessage, not(containsString("for property")));
    }

    @Test
    void noteInNotebookWithoutTheMapHasOnlyTheGeneralInstruction() {
      Notebook other = makeMe.aNotebook().please();
      makeMe
          .theNotebook(other)
          .readmeContent("---\nquestion_generation_instruction: GENERAL_INSTRUCTION\n---\n")
          .please();

      String instructionMessage =
          firstUserMessage(noteWithPartOfAndSimilarTo(other), new PropertyFocus("a part of", ""));

      assertThat(instructionMessage, containsString("GENERAL_INSTRUCTION"));
      assertThat(instructionMessage, not(containsString("PART_OF_INSTRUCTION")));
    }

    @Test
    void relationshipNoteCarriesTheEntryForItsRelation() {
      Note relationship = relationshipNote(note.getNotebook(), "a-part-of");

      String instructionMessage = firstUserMessage(relationship, null);

      assertThat(instructionMessage, containsString(PART_OF_LABEL + "\nPART_OF_INSTRUCTION"));
      assertThat(
          instructionMessage.indexOf("GENERAL_INSTRUCTION"),
          lessThan(instructionMessage.indexOf(PART_OF_LABEL)));
    }

    @Test
    void relationshipNoteOfOtherRelationHasOnlyTheGeneralInstruction() {
      String instructionMessage =
          firstUserMessage(relationshipNote(note.getNotebook(), "similar-to"), null);

      assertThat(instructionMessage, containsString("GENERAL_INSTRUCTION"));
      assertThat(instructionMessage, not(containsString("for property")));
    }

    @Test
    void anExampleOfRelationCarriesTheExampleOfEntry() {
      Note relationship =
          relationshipNote(notebookWithMap("example of: EXAMPLE_OF_INSTRUCTION"), "an-example-of");

      assertThat(
          firstUserMessage(relationship, null),
          containsString(
              "Instruction from notebook \"Anatomy\" for property \"example of\":"
                  + "\nEXAMPLE_OF_INSTRUCTION"));
    }

    @Test
    void relationshipNoteUsesItsOwnNotebooksMap() {
      Notebook other = makeMe.aNotebook().please();
      makeMe
          .theNotebook(other)
          .readmeContent("---\nquestion_generation_instruction: GENERAL_INSTRUCTION\n---\n")
          .please();

      String instructionMessage = firstUserMessage(relationshipNote(other, "a-part-of"), null);

      assertThat(instructionMessage, containsString("GENERAL_INSTRUCTION"));
      assertThat(instructionMessage, not(containsString("PART_OF_INSTRUCTION")));
    }

    private Note relationshipNote(Notebook nb, String relation) {
      return makeMe
          .aNote()
          .notebook(nb)
          .content(
              "---\ntype: Relationship\nrelation: "
                  + relation
                  + "\nsource: \"[[Hand]]\"\ntarget: \"[[Body]]\"\n---\n")
          .please();
    }

    private Notebook notebookWithMap(String mapEntry) {
      Notebook nb = makeMe.aNotebook().name("Anatomy").please();
      makeMe
          .theNotebook(nb)
          .readmeContent(
              "---\nquestion_generation_instruction: GENERAL_INSTRUCTION\n"
                  + "question_generation_instructions_by_property:\n  "
                  + mapEntry
                  + "\n---\n")
          .please();
      return nb;
    }

    private Note noteWithPartOfAndSimilarTo(Notebook nb) {
      return makeMe
          .aNote()
          .notebook(nb)
          .content(
              "---\na part of: \"[[Body]]\"\na part of 2: \"[[Arm]]\"\n"
                  + "similar to: \"[[Leg]]\"\n---\nHand")
          .please();
    }

    private String firstUserMessage(Note focus, PropertyFocus propertyFocus) {
      return userMessageContentStrings(
              requestBuilder.buildQuestionGenerationResponseRequest(
                  focus, null, null, propertyFocus))
          .get(0);
    }
  }
}
