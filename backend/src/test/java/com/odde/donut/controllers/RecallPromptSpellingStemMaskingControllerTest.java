package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.controllers.dto.SpellingQuestion;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecallPromptSpellingStemMaskingControllerTest extends RecallPromptControllerTestBase {
  Note answerNote;
  MemoryTracker memoryTracker;

  @BeforeEach
  void setup() {
    answerNote = ownedNote();
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
            overlaps:
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
  void spellingQuestionDoesNotMaskOverlapTargetTitleFromOverlapsOnly() {
    makeMe
        .theNote(answerNote)
        .title("colour")
        .content(
            """
            ---
            overlaps:
              - "[[Other Note]]"
            ---
            Mentions Other Note in the body
            """)
        .please();

    SpellingQuestion question = spellingPrompt(memoryTracker).getSpellingQuestion();

    assertThat(question.getStem(), containsString("Other Note"));
    assertThat(question.getStem(), not(containsString("<mark")));
  }

  @Test
  void spellingQuestionMasksWholeSlashTitleButNotSeparateSegments() {
    makeMe
        .theNote(answerNote)
        .title("word／~logical")
        .content("The exact word／~logical term differs from a separate word or logical mention.")
        .please();

    SpellingQuestion question = spellingPrompt(memoryTracker).getSpellingQuestion();

    assertThat(question.getStem(), not(containsString("word／~logical")));
    assertThat(question.getStem(), containsString("a separate word or logical mention"));
  }

  @Test
  void spellingQuestionStillMasksLeadingTildeSuffix() {
    makeMe.theNote(answerNote).title("~logical").content("This claim is illogical.").please();

    SpellingQuestion question = spellingPrompt(memoryTracker).getSpellingQuestion();

    assertThat(question.getStem(), containsString("<mark"));
    assertThat(question.getStem(), not(containsString("logical.")));
  }

  @Test
  void spellingQuestionMasksTitleTextBeforeTrailingQualifier() {
    makeMe
        .theNote(answerNote)
        .title("Deposition (phase transition) ")
        .content("In physics, deposition is when gas becomes solid. Deposition skips liquid.")
        .please();

    SpellingQuestion question = spellingPrompt(memoryTracker).getSpellingQuestion();

    assertThat(
        stemWithoutMarkTags(question),
        equalTo("In physics, [...] is when gas becomes solid. [...] skips liquid."));
  }

  @Test
  void spellingQuestionMasksWholeQualifiedTitleAsOneMaskAndKeepsQualifierTextReadable() {
    makeMe
        .theNote(answerNote)
        .title("cat(animal)")
        .content("A cat is an animal. See cat(animal).")
        .please();

    SpellingQuestion question = spellingPrompt(memoryTracker).getSpellingQuestion();

    assertThat(stemWithoutMarkTags(question), equalTo("A [...] is an animal. See [...]."));
  }

  @Test
  void spellingQuestionLeavesClueReadableWhenNoTitleTextPrecedesTheQualifier() {
    makeMe.theNote(answerNote).title("~(phase transition)").content("Gas becomes solid.").please();

    SpellingQuestion question = spellingPrompt(memoryTracker).getSpellingQuestion();

    assertThat(stemWithoutMarkTags(question), equalTo("Gas becomes solid."));
  }

  private static String stemWithoutMarkTags(SpellingQuestion question) {
    return question.getStem().replaceAll("</?mark[^>]*>", "");
  }
}
