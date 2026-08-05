package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.controllers.dto.SpellingQuestion;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecallPromptSpellingStemMaskingControllerTest extends RecallPromptControllerTestBase {
  Note answerNote;
  MemoryTracker memoryTracker;

  @BeforeEach
  void setup() {
    answerNote = ownedSpellingNote();
    memoryTracker = ownedSpellingTracker(answerNote);
  }

  @Test
  void spellingQuestionMasksFrontmatterAliasesInStem() {
    makeMe
        .theNote(answerNote)
        .title("colour")
        .content(
            """
            ---
            aliases:
              - color
            ---
            The color of the sky is blue
            """)
        .please();

    SpellingQuestion question = spellingPrompt(memoryTracker).getSpellingQuestion();

    assertThat(question.getStem(), containsString("<mark"));
    assertThat(question.getStem(), not(containsString("color")));
  }

  @Test
  void spellingQuestionMasksPlainAliasButNotOverlapWikiLinkTargetTitle() {
    makeMe
        .theNote(answerNote)
        .title("colour")
        .content(
            """
            ---
            aliases:
              - color
              - "[[Other Note]]"
            ---
            The color of Other Note is blue
            """)
        .please();

    SpellingQuestion question = spellingPrompt(memoryTracker).getSpellingQuestion();

    assertThat(question.getStem(), not(containsString("color")));
    assertThat(question.getStem(), containsString("Other Note"));
  }

  @Test
  void spellingQuestionDoesNotMaskOverlapTargetTitleFromWikiLinkOnlyAlias() {
    makeMe
        .theNote(answerNote)
        .title("colour")
        .content(
            """
            ---
            aliases:
              - "[[Other Note]]"
            ---
            Mentions Other Note in the body
            """)
        .please();

    SpellingQuestion question = spellingPrompt(memoryTracker).getSpellingQuestion();

    assertThat(question.getStem(), containsString("Other Note"));
    assertThat(question.getStem(), not(containsString("<mark")));
  }
}
