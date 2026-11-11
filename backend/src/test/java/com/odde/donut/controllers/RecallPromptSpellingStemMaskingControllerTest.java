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
    SpellingQuestion question =
        spellingQuestionFor(
            "colour",
            """
            ---
            aliases:
              - color
            ---
            The color of the sky is blue
            """);

    assertThat(question.getStem(), containsString("<mark"));
    assertThat(question.getStem(), not(containsString("color")));
  }

  @Test
  void spellingQuestionMasksPlainAliasButNotOverlapWikiLinkTargetTitle() {
    SpellingQuestion question =
        spellingQuestionFor(
            "colour",
            """
            ---
            aliases:
              - color
            overlaps:
              - "[[Other Note]]"
            ---
            The color of Other Note is blue
            """);

    assertThat(question.getStem(), not(containsString("color")));
    assertThat(question.getStem(), containsString("Other Note"));
  }

  @Test
  void spellingQuestionDoesNotMaskOverlapTargetTitleFromOverlapsOnly() {
    SpellingQuestion question =
        spellingQuestionFor(
            "colour",
            """
            ---
            overlaps:
              - "[[Other Note]]"
            ---
            Mentions Other Note in the body
            """);

    assertThat(question.getStem(), containsString("Other Note"));
    assertThat(question.getStem(), not(containsString("<mark")));
  }

  @Test
  void spellingQuestionMasksWholeSlashTitleButNotSeparateSegments() {
    SpellingQuestion question =
        spellingQuestionFor(
            "word／~logical",
            "The exact word／~logical term differs from a separate word or logical mention.");

    assertThat(question.getStem(), not(containsString("word／~logical")));
    assertThat(question.getStem(), containsString("a separate word or logical mention"));
  }

  @Test
  void spellingQuestionStillMasksLeadingTildeSuffix() {
    SpellingQuestion question = spellingQuestionFor("~logical", "This claim is illogical.");

    assertThat(question.getStem(), containsString("<mark"));
    assertThat(question.getStem(), not(containsString("logical.")));
  }

  @Test
  void spellingQuestionMasksTitleTextBeforeTrailingQualifier() {
    SpellingQuestion question =
        spellingQuestionFor(
            "Deposition (phase transition) ",
            "In physics, deposition is when gas becomes solid. Deposition skips liquid.");

    assertThat(
        stemWithoutMarkTags(question),
        equalTo("In physics, [...] is when gas becomes solid. [...] skips liquid."));
  }

  @Test
  void spellingQuestionMasksWholeQualifiedTitleAsOneMaskAndKeepsQualifierTextReadable() {
    SpellingQuestion question =
        spellingQuestionFor("cat(animal)", "A cat is an animal. See cat(animal).");

    assertThat(stemWithoutMarkTags(question), equalTo("A [...] is an animal. See [...]."));
  }

  @Test
  void spellingQuestionLeavesClueReadableWhenNoTitleTextPrecedesTheQualifier() {
    SpellingQuestion question = spellingQuestionFor("~(phase transition)", "Gas becomes solid.");

    assertThat(stemWithoutMarkTags(question), equalTo("Gas becomes solid."));
  }

  @Test
  void spellingQuestionHidesSavedPronunciationWhereTheBodyRepeatsIt() {
    SpellingQuestion question =
        spellingQuestionFor(
            "多大",
            """
            ---
            pronunciation: ただい
            ---
            数や量が非常に大きいこと。「多大（ただい）なるご迷惑」\
            """);

    assertThat(stemWithoutMarkTags(question), equalTo("数や量が非常に大きいこと。「[...]（/.../）なるご迷惑」"));
    assertThat(
        question.getStem(), containsString("<mark title='Hidden pronunciation'>/.../</mark>"));
  }

  @Test
  void spellingQuestionHidesEveryValueOfAHandAuthoredPronunciationList() {
    SpellingQuestion question =
        spellingQuestionFor(
            "顔色",
            """
            ---
            pronunciation:
              - かおいろ
              - がんしょく
            ---
            顔色（かおいろ）が悪い。顔色（がんしょく）を失う。\
            """);

    assertThat(stemWithoutMarkTags(question), equalTo("[...]（/.../）が悪い。[...]（/.../）を失う。"));
  }

  @Test
  void spellingQuestionHidesALongerSavedPronunciationBeforeAShorterOneItStartsWith() {
    SpellingQuestion question =
        spellingQuestionFor("人", "---\npronunciation: [ひと, ひとびと]\n---\n人々（ひとびと）と人（ひと）");

    assertThat(stemWithoutMarkTags(question), equalTo("[...]々（/.../）と[...]（/.../）"));
  }

  @Test
  void spellingQuestionHidesAOneKanaSavedPronunciationInsideOtherWords() {
    SpellingQuestion question = spellingQuestionFor("手", "---\npronunciation: て\n---\n手を挙げて。");

    assertThat(stemWithoutMarkTags(question), equalTo("[...]を挙げ/.../。"));
  }

  @Test
  void spellingQuestionHidesBothASavedPronunciationAndASlashReadingInTheBody() {
    SpellingQuestion question =
        spellingQuestionFor("多大", "---\npronunciation: ただい\n---\n多大 /ただい/ は「ただい」と読む。");

    assertThat(stemWithoutMarkTags(question), equalTo("[...] /.../ は「/.../」と読む。"));
  }

  @Test
  void spellingQuestionIgnoresABlankSavedPronunciation() {
    SpellingQuestion question =
        spellingQuestionFor("sky", "---\npronunciation: \"\"\n---\nThe sky is blue.");

    assertThat(stemWithoutMarkTags(question), equalTo("The [...] is blue."));
  }

  private SpellingQuestion spellingQuestionFor(String title, String content) {
    makeMe.theNote(answerNote).title(title).content(content).please();
    return spellingPrompt(memoryTracker).getSpellingQuestion();
  }

  private static String stemWithoutMarkTags(SpellingQuestion question) {
    return question.getStem().replaceAll("</?mark[^>]*>", "");
  }
}
