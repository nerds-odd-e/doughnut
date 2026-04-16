package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

class JapaneseAuxiliaryClozeTest {

  private static final ClozeReplacement CLOZE =
      new ClozeReplacement("[..~]", "[...]", "/.../", "<...>");

  @Test
  void suffixTitle_keepsAuxiliaryAfterStemMatch() {
    String result =
        new ClozedString(CLOZE, "昨日は魚を食べた。")
            .hide(new NoteTitle("~食べ"))
            .maskedDetailsAsMarkdown();
    assertThat(result, containsString("[..~]た。"));
  }

  @Test
  void suffixTitle_teForm_keepsTe() {
    String result =
        new ClozedString(CLOZE, "魚を食べてください。")
            .hide(new NoteTitle("~食べ"))
            .maskedDetailsAsMarkdown();
    assertThat(result, containsString("[..~]てください。"));
  }

  @Test
  void suffixTitle_politeNegative_keepsFullAuxiliary() {
    String result =
        new ClozedString(CLOZE, "彼は食べませんでした。")
            .hide(new NoteTitle("~食べ"))
            .maskedDetailsAsMarkdown();
    assertThat(result, containsString("[..~]ませんでした。"));
  }

  @Test
  void suffixTitle_iAdjective_keepsKuNai() {
    String result =
        new ClozedString(CLOZE, "この部屋は高くない。")
            .hide(new NoteTitle("~高"))
            .maskedDetailsAsMarkdown();
    assertThat(result, containsString("[..~]くない。"));
  }

  @Test
  void suffixTitle_naAdjectiveCopula_keepsDa() {
    String result =
        new ClozedString(CLOZE, "この町は静かだ。")
            .hide(new NoteTitle("~静か"))
            .maskedDetailsAsMarkdown();
    assertThat(result, containsString("[..~]だ。"));
  }

  @Test
  void suffixTitle_naAdjectiveNegative_keepsDeHaNai() {
    String result =
        new ClozedString(CLOZE, "ここは静かではない。")
            .hide(new NoteTitle("~静か"))
            .maskedDetailsAsMarkdown();
    assertThat(result, containsString("[..~]ではない。"));
  }

  @Test
  void suffixTitle_passive_keepsTaAfterRareruStem() {
    String result =
        new ClozedString(CLOZE, "彼は認められた。")
            .hide(new NoteTitle("~認められ"))
            .maskedDetailsAsMarkdown();
    assertThat(result, containsString("[..~]た。"));
  }
}
